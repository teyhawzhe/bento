package com.lovius.bento.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lovius.bento.dao.MonthlyBillingLogRepository;
import com.lovius.bento.dao.MonthlyBillingReportRepository;
import com.lovius.bento.dto.MonthlyBillingTriggerResponse;
import com.lovius.bento.model.MonthlyBillingAggregationRow;
import com.lovius.bento.model.MonthlyBillingLog;
import com.lovius.bento.model.MonthlyBillingPeriod;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MonthlyBillingServiceTest {
    @Mock
    private MonthlyBillingReportRepository monthlyBillingReportRepository;

    @Mock
    private MonthlyBillingLogRepository monthlyBillingLogRepository;

    @Mock
    private BillingReportRecipientProvider billingReportRecipientProvider;

    @Mock
    private EmailService emailService;

    private MonthlyBillingService monthlyBillingService;

    @BeforeEach
    void setUp() {
        monthlyBillingService = new MonthlyBillingService(
                monthlyBillingReportRepository,
                monthlyBillingLogRepository,
                new MonthlyBillingPeriodService(),
                billingReportRecipientProvider,
                emailService);
        when(monthlyBillingLogRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void runMonthlyBillingBuildsSupplierTotalsAndSendsToAllRecipients() {
        MonthlyBillingPeriod period = new MonthlyBillingPeriod(
                LocalDate.of(2026, 2, 15),
                LocalDate.of(2026, 3, 14));
        when(monthlyBillingReportRepository.findBillingRows(period.startDate(), period.endDate())).thenReturn(List.of(
                new MonthlyBillingAggregationRow(1L, "月結便當", "supplier@company.local", "排骨便當", new BigDecimal("120.00"), 2),
                new MonthlyBillingAggregationRow(1L, "月結便當", "supplier@company.local", "雞腿便當", new BigDecimal("130.00"), 1)));
        when(billingReportRecipientProvider.getRecipientEmails()).thenReturn(List.of("finance@company.local"));

        MonthlyBillingTriggerResponse response = monthlyBillingService.runMonthlyBilling(period, 9L);

        assertEquals(1, response.supplierCount());
        assertEquals(2, response.recipientCount());
        assertEquals(0, response.failedCount());
        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService, times(2)).sendEmail(anyString(), anyString(), bodyCaptor.capture());
        assertTrue(bodyCaptor.getValue().contains("TOTAL"));
        assertTrue(bodyCaptor.getValue().contains("370"));
    }

    @Test
    void runMonthlyBillingContinuesWhenOneRecipientFails() {
        MonthlyBillingPeriod period = new MonthlyBillingPeriod(
                LocalDate.of(2026, 2, 15),
                LocalDate.of(2026, 3, 14));
        when(monthlyBillingReportRepository.findBillingRows(period.startDate(), period.endDate())).thenReturn(List.of(
                new MonthlyBillingAggregationRow(1L, "月結便當", "supplier@company.local", "排骨便當", new BigDecimal("120.00"), 1)));
        when(billingReportRecipientProvider.getRecipientEmails()).thenReturn(List.of("finance@company.local"));
        doThrow(new RuntimeException("smtp down"))
                .when(emailService)
                .sendEmail(eq("supplier@company.local"), anyString(), anyString());

        MonthlyBillingTriggerResponse response = monthlyBillingService.runMonthlyBilling(period, 9L);

        assertEquals(2, response.recipientCount());
        assertEquals(1, response.failedCount());
        verify(emailService).sendEmail(eq("finance@company.local"), anyString(), anyString());
        ArgumentCaptor<MonthlyBillingLog> logCaptor = ArgumentCaptor.forClass(MonthlyBillingLog.class);
        verify(monthlyBillingLogRepository, times(2)).save(logCaptor.capture());
        assertEquals("failed", logCaptor.getAllValues().get(0).getStatus());
        assertEquals("sent", logCaptor.getAllValues().get(1).getStatus());
    }
}
