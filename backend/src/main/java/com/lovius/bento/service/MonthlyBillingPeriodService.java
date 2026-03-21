package com.lovius.bento.service;

import com.lovius.bento.model.MonthlyBillingPeriod;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.stereotype.Service;

@Service
public class MonthlyBillingPeriodService {
    private final Clock clock;

    public MonthlyBillingPeriodService() {
        this(Clock.systemDefaultZone());
    }

    MonthlyBillingPeriodService(Clock clock) {
        this.clock = clock;
    }

    public MonthlyBillingPeriod currentPeriod() {
        return calculatePeriod(LocalDate.now(clock));
    }

    public MonthlyBillingPeriod calculatePeriod(LocalDate executionDate) {
        LocalDate endDate = executionDate.withDayOfMonth(14);
        LocalDate startDate = executionDate.minusMonths(1).withDayOfMonth(15);
        return new MonthlyBillingPeriod(startDate, endDate);
    }
}
