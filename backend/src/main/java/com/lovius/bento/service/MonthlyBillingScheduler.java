package com.lovius.bento.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MonthlyBillingScheduler {
    private final MonthlyBillingService monthlyBillingService;

    public MonthlyBillingScheduler(MonthlyBillingService monthlyBillingService) {
        this.monthlyBillingService = monthlyBillingService;
    }

    @Scheduled(cron = "0 0 0 15 * *")
    public void runMonthlyBilling() {
        monthlyBillingService.runMonthlyBilling(null);
    }
}
