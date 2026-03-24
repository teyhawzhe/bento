package com.lovius.bento.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lovius.bento.dao.NotificationLogRepository;
import com.lovius.bento.dao.OrderRepository;
import com.lovius.bento.model.NotificationLog;
import com.lovius.bento.model.SupplierOrderNotificationRow;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SupplierOrderNotificationServiceTest {
    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Taipei");

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private NotificationLogRepository notificationLogRepository;

    @Mock
    private ErrorNotificationRecipientProvider errorNotificationRecipientProvider;

    @Mock
    private EmailService emailService;

    private SupplierOrderNotificationService supplierOrderNotificationService;

    @BeforeEach
    void setUp() {
        supplierOrderNotificationService = new SupplierOrderNotificationService(
                orderRepository,
                notificationLogRepository,
                errorNotificationRecipientProvider,
                emailService,
                fixedClock("2026-03-24T17:00:00+08:00"));
        when(notificationLogRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void runTomorrowNotificationsSendsSupplierEmailAndPersistsSuccessLog() {
        when(orderRepository.findSupplierOrderNotificationRows(LocalDate.of(2026, 3, 25))).thenReturn(List.of(
                new SupplierOrderNotificationRow(1L, "好食便當", "supplier@company.local", "雞腿便當", 3),
                new SupplierOrderNotificationRow(1L, "好食便當", "supplier@company.local", "排骨便當", 2)));

        supplierOrderNotificationService.runTomorrowNotifications();

        verify(emailService).sendEmail(eq("supplier@company.local"), anyString(), anyString());
        ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(notificationLogRepository).save(captor.capture());
        assertEquals("success", captor.getValue().getStatus());
        assertEquals("supplier@company.local", captor.getValue().getEmailTo());
    }

    @Test
    void runTomorrowNotificationsNotifiesErrorRecipientsWhenSupplierSendThrows() {
        when(orderRepository.findSupplierOrderNotificationRows(LocalDate.of(2026, 3, 25))).thenReturn(List.of(
                new SupplierOrderNotificationRow(1L, "好食便當", "supplier@company.local", "雞腿便當", 3)));
        when(errorNotificationRecipientProvider.getRecipientEmails()).thenReturn(List.of("ops@company.local"));
        doThrow(new RuntimeException("smtp down"))
                .when(emailService)
                .sendEmail(eq("supplier@company.local"), anyString(), anyString());

        supplierOrderNotificationService.runTomorrowNotifications();

        verify(emailService).sendEmail(eq("ops@company.local"), anyString(), anyString());
        ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(notificationLogRepository, times(2)).save(captor.capture());
        assertEquals("exception", captor.getAllValues().get(0).getStatus());
        assertEquals("exception", captor.getAllValues().get(1).getStatus());
    }

    @Test
    void runTomorrowNotificationsWritesSystemErrorLogsWhenQueryFails() {
        when(orderRepository.findSupplierOrderNotificationRows(LocalDate.of(2026, 3, 25)))
                .thenThrow(new RuntimeException("db down"));
        when(errorNotificationRecipientProvider.getRecipientEmails()).thenReturn(List.of("ops-a@company.local", "ops-b@company.local"));

        supplierOrderNotificationService.runTomorrowNotifications();

        verify(emailService).sendEmail(eq("ops-a@company.local"), anyString(), anyString());
        verify(emailService).sendEmail(eq("ops-b@company.local"), anyString(), anyString());
        ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(notificationLogRepository, times(2)).save(captor.capture());
        assertEquals("system_error", captor.getAllValues().get(0).getStatus());
        assertEquals("db down", captor.getAllValues().get(0).getErrorMessage());
    }

    @Test
    void runTomorrowNotificationsMarksMissingSupplierEmailAsFailed() {
        when(orderRepository.findSupplierOrderNotificationRows(LocalDate.of(2026, 3, 25))).thenReturn(List.of(
                new SupplierOrderNotificationRow(1L, "好食便當", "", "雞腿便當", 3)));
        when(errorNotificationRecipientProvider.getRecipientEmails()).thenReturn(List.of("ops@company.local"));

        supplierOrderNotificationService.runTomorrowNotifications();

        verify(emailService, never()).sendEmail(eq(""), anyString(), anyString());
        verify(emailService).sendEmail(eq("ops@company.local"), anyString(), anyString());
        ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(notificationLogRepository, times(2)).save(captor.capture());
        assertEquals("failed", captor.getAllValues().get(0).getStatus());
    }

    private Clock fixedClock(String isoOffsetDateTime) {
        return Clock.fixed(OffsetDateTime.parse(isoOffsetDateTime).toInstant(), ZONE_ID);
    }
}
