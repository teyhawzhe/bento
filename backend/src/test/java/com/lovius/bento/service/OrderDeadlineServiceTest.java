package com.lovius.bento.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lovius.bento.exception.ApiException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;

class OrderDeadlineServiceTest {
    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Taipei");

    @Test
    void nextWeekdaysReturnsUpcomingMondayToFriday() {
        OrderDeadlineService service = new OrderDeadlineService(fixedClock("2026-03-23T09:00:00+08:00"));

        List<LocalDate> weekdays = service.nextWeekdays();

        assertEquals(List.of(
                LocalDate.of(2026, 3, 30),
                LocalDate.of(2026, 3, 31),
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 2),
                LocalDate.of(2026, 4, 3)), weekdays);
    }

    @Test
    void employeeOrderWindowOpenBeforeFridayNoon() {
        OrderDeadlineService service = new OrderDeadlineService(fixedClock("2026-03-27T11:59:00+08:00"));

        assertDoesNotThrow(service::ensureEmployeeOrderWindowOpen);
        assertTrue(service.isEmployeeOrderWindowOpen());
    }

    @Test
    void employeeOrderWindowClosedAtFridayNoon() {
        OrderDeadlineService service = new OrderDeadlineService(fixedClock("2026-03-27T12:00:00+08:00"));

        ApiException exception = assertThrows(ApiException.class, service::ensureEmployeeOrderWindowOpen);

        assertEquals("已超過訂餐截止時間", exception.getMessage());
        assertFalse(service.isEmployeeOrderWindowOpen());
    }

    @Test
    void adminCancellationWindowOpenBeforePreviousDayFivePm() {
        OrderDeadlineService service = new OrderDeadlineService(fixedClock("2026-03-31T16:59:00+08:00"));

        assertDoesNotThrow(() -> service.ensureAdminCancellationWindowOpen(LocalDate.of(2026, 4, 1)));
    }

    @Test
    void adminCancellationWindowClosedAtPreviousDayFivePm() {
        OrderDeadlineService service = new OrderDeadlineService(fixedClock("2026-03-31T17:00:00+08:00"));

        ApiException exception = assertThrows(
                ApiException.class,
                () -> service.ensureAdminCancellationWindowOpen(LocalDate.of(2026, 4, 1)));

        assertEquals("已超過管理員取消訂餐截止時間", exception.getMessage());
    }

    private Clock fixedClock(String isoOffsetDateTime) {
        return Clock.fixed(OffsetDateTime.parse(isoOffsetDateTime).toInstant(), ZONE_ID);
    }
}
