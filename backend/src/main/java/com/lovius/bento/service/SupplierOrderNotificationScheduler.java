package com.lovius.bento.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SupplierOrderNotificationScheduler {
    private final SupplierOrderNotificationService supplierOrderNotificationService;

    public SupplierOrderNotificationScheduler(SupplierOrderNotificationService supplierOrderNotificationService) {
        this.supplierOrderNotificationService = supplierOrderNotificationService;
    }

    @Scheduled(cron = "0 0 17 * * *")
    public void runDailyNotification() {
        supplierOrderNotificationService.runTomorrowNotifications();
    }
}
