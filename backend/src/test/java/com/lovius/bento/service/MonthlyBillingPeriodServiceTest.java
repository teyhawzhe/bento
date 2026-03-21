package com.lovius.bento.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.lovius.bento.model.MonthlyBillingPeriod;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class MonthlyBillingPeriodServiceTest {

    @Test
    void calculatePeriodUsesPreviousMonth15ToCurrentMonth14() {
        Clock clock = Clock.fixed(Instant.parse("2026-03-21T10:00:00Z"), ZoneId.of("UTC"));
        MonthlyBillingPeriodService service = new MonthlyBillingPeriodService(clock);

        MonthlyBillingPeriod period = service.currentPeriod();

        assertEquals(LocalDate.of(2026, 2, 15), period.startDate());
        assertEquals(LocalDate.of(2026, 3, 14), period.endDate());
    }
}
