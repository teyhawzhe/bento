package com.lovius.bento.service;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MenuCheckNotificationSchedulerTest {
    @Mock
    private MenuCheckNotificationService menuCheckNotificationService;

    @Test
    void schedulerDelegatesToMenuCheckService() {
        MenuCheckNotificationScheduler scheduler = new MenuCheckNotificationScheduler(menuCheckNotificationService);

        scheduler.runDailyMenuCheck();

        verify(menuCheckNotificationService).runDailyCheck();
    }
}
