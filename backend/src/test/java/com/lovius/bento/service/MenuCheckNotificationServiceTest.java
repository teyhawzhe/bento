package com.lovius.bento.service;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;

import com.lovius.bento.dao.MenuNotificationDismissRepository;
import com.lovius.bento.dao.MenuNotificationLogRepository;
import com.lovius.bento.dao.MenuRepository;
import com.lovius.bento.dao.WorkCalendarRepository;
import com.lovius.bento.model.Menu;
import com.lovius.bento.model.MenuNotificationDismiss;
import com.lovius.bento.model.MenuNotificationLog;
import com.lovius.bento.model.WorkCalendar;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MenuCheckNotificationServiceTest {
    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Taipei");

    @Mock
    private WorkCalendarRepository workCalendarRepository;

    @Mock
    private MenuRepository menuRepository;

    @Mock
    private MenuNotificationLogRepository menuNotificationLogRepository;

    @Mock
    private MenuNotificationDismissRepository menuNotificationDismissRepository;

    @Mock
    private ErrorNotificationRecipientProvider errorNotificationRecipientProvider;

    @Mock
    private EmailService emailService;

    @Test
    void getMissingDatesForAdminReturnsEmptyWhenDismissed() {
        LocalDate today = LocalDate.of(2026, 4, 2);
        MenuCheckNotificationService service = createService(today);
        when(menuNotificationDismissRepository.existsByDismissDate(today)).thenReturn(true);

        Assertions.assertTrue(service.getMissingDatesForAdmin().isEmpty());
    }

    @Test
    void getMissingDatesForAdminReturnsDatesWithoutMenus() {
        LocalDate today = LocalDate.of(2026, 4, 2);
        LocalDate endDate = today.plusMonths(1);
        MenuCheckNotificationService service = createService(today);
        when(menuNotificationDismissRepository.existsByDismissDate(today)).thenReturn(false);
        when(workCalendarRepository.findByDateRange(today, endDate)).thenReturn(List.of(
                new WorkCalendar(LocalDate.of(2026, 4, 3), true),
                new WorkCalendar(LocalDate.of(2026, 4, 4), false),
                new WorkCalendar(LocalDate.of(2026, 4, 6), true)));
        when(menuRepository.findAll(true, today, null)).thenReturn(List.of(
                menu(1L, LocalDate.of(2026, 4, 3), LocalDate.of(2026, 4, 3))));

        List<LocalDate> missingDates = service.getMissingDatesForAdmin();

        Assertions.assertEquals(List.of(LocalDate.of(2026, 4, 6)), missingDates);
    }

    @Test
    void dismissTodaySavesDismissDate() {
        LocalDate today = LocalDate.of(2026, 4, 2);
        MenuCheckNotificationService service = createService(today);

        service.dismissToday();

        ArgumentCaptor<MenuNotificationDismiss> captor = ArgumentCaptor.forClass(MenuNotificationDismiss.class);
        verify(menuNotificationDismissRepository).save(captor.capture());
        Assertions.assertEquals(today, captor.getValue().getDismissDate());
    }

    @Test
    void runDailyCheckSendsOnceAndWritesSuccessLog() {
        LocalDate today = LocalDate.of(2026, 4, 2);
        LocalDate endDate = today.plusMonths(1);
        MenuCheckNotificationService service = createService(today);
        when(workCalendarRepository.findByDateRange(today, endDate)).thenReturn(List.of(
                new WorkCalendar(LocalDate.of(2026, 4, 3), true),
                new WorkCalendar(LocalDate.of(2026, 4, 4), true)));
        when(menuRepository.findAll(true, today, null)).thenReturn(List.of(menu(
                1L,
                LocalDate.of(2026, 4, 3),
                LocalDate.of(2026, 4, 3))));
        when(menuNotificationLogRepository.existsByNotifyDate(today)).thenReturn(false);
        when(errorNotificationRecipientProvider.getRecipientEmails()).thenReturn(List.of("ops@company.local"));

        service.runDailyCheck();

        verify(emailService).sendEmail(
                eq("ops@company.local"),
                eq("A015 菜單設定提醒 2026-04-02"),
                contains("2026-04-04"));
        ArgumentCaptor<MenuNotificationLog> captor = ArgumentCaptor.forClass(MenuNotificationLog.class);
        verify(menuNotificationLogRepository).save(captor.capture());
        Assertions.assertEquals("success", captor.getValue().getStatus());
        Assertions.assertEquals(LocalDate.of(2026, 4, 4), captor.getValue().getMissingFrom());
    }

    @Test
    void runDailyCheckSkipsWhenAlreadyLoggedToday() {
        LocalDate today = LocalDate.of(2026, 4, 2);
        LocalDate endDate = today.plusMonths(1);
        MenuCheckNotificationService service = createService(today);
        when(workCalendarRepository.findByDateRange(today, endDate)).thenReturn(List.of(
                new WorkCalendar(LocalDate.of(2026, 4, 3), true)));
        when(menuRepository.findAll(true, today, null)).thenReturn(List.of());
        when(menuNotificationLogRepository.existsByNotifyDate(today)).thenReturn(true);

        service.runDailyCheck();

        verify(emailService, never()).sendEmail(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString());
    }

    private MenuCheckNotificationService createService(LocalDate today) {
        return new MenuCheckNotificationService(
                workCalendarRepository,
                menuRepository,
                menuNotificationLogRepository,
                menuNotificationDismissRepository,
                errorNotificationRecipientProvider,
                emailService,
                Clock.fixed(today.atStartOfDay(ZONE_ID).toInstant(), ZONE_ID));
    }

    private Menu menu(Long id, LocalDate validFrom, LocalDate validTo) {
        return new Menu(
                id,
                1L,
                "排骨便當",
                "肉類",
                null,
                BigDecimal.valueOf(120),
                validFrom,
                validTo,
                1L,
                Instant.parse("2026-04-01T00:00:00Z"),
                Instant.parse("2026-04-01T00:00:00Z"));
    }
}
