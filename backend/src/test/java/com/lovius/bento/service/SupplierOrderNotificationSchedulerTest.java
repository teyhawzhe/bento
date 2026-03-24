package com.lovius.bento.service;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SupplierOrderNotificationSchedulerTest {
    @Mock
    private SupplierOrderNotificationService supplierOrderNotificationService;

    @Test
    void schedulerDelegatesToTomorrowNotificationService() {
        SupplierOrderNotificationScheduler scheduler = new SupplierOrderNotificationScheduler(
                supplierOrderNotificationService);

        scheduler.runDailyNotification();

        verify(supplierOrderNotificationService).runTomorrowNotifications();
    }
}
