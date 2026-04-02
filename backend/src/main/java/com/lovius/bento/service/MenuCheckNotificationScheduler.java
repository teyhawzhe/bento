package com.lovius.bento.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MenuCheckNotificationScheduler {
    private final MenuCheckNotificationService menuCheckNotificationService;

    public MenuCheckNotificationScheduler(MenuCheckNotificationService menuCheckNotificationService) {
        this.menuCheckNotificationService = menuCheckNotificationService;
    }

    @Scheduled(cron = "0 0 8 * * *")
    public void runDailyMenuCheck() {
        menuCheckNotificationService.runDailyCheck();
    }
}
