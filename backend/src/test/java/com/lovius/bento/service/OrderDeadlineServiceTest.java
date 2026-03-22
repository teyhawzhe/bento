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
    void employeeOrderableDatesStartAfterUpcomingFridayDeadline() {
        OrderDeadlineService service = new OrderDeadlineService(fixedClock("2026-03-23T09:00:00+08:00"));

        List<LocalDate> orderableDates = service.employeeOrderableDates();

        assertEquals(List.of(
                LocalDate.of(2026, 3, 28),
                LocalDate.of(2026, 3, 29),
                LocalDate.of(2026, 3, 30),
                LocalDate.of(2026, 3, 31),
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 2),
                LocalDate.of(2026, 4, 3)), orderableDates);
    }

    @Test
    void employeeOrderableDatesRollForwardAfterFridayNoon() {
        OrderDeadlineService service = new OrderDeadlineService(fixedClock("2026-03-27T12:00:00+08:00"));

        List<LocalDate> orderableDates = service.employeeOrderableDates();

        assertEquals(LocalDate.of(2026, 4, 4), orderableDates.get(0));
        assertEquals(LocalDate.of(2026, 4, 10), orderableDates.get(orderableDates.size() - 1));
    }

    @Test
    void employeeOrderableDateAllowsCurrentCycleDate() {
        OrderDeadlineService service = new OrderDeadlineService(fixedClock("2026-03-23T16:59:00+08:00"));

        assertDoesNotThrow(() -> service.ensureEmployeeOrderableDate(LocalDate.of(2026, 3, 30)));
    }

    @Test
    void employeeOrderableDateRejectsDateBeforeCurrentCycle() {
        OrderDeadlineService service = new OrderDeadlineService(fixedClock("2026-03-23T09:00:00+08:00"));

        ApiException exception = assertThrows(
                ApiException.class,
                () -> service.ensureEmployeeOrderableDate(LocalDate.of(2026, 3, 23)));

        assertEquals("僅可訂購本次開放區間（截止日後至下週五）的便當", exception.getMessage());
    }

    @Test
    void employeeOrderableDateAllowsWeekendWhenInCurrentCycle() {
        OrderDeadlineService service = new OrderDeadlineService(fixedClock("2026-03-23T09:00:00+08:00"));

        assertDoesNotThrow(() -> service.ensureEmployeeOrderableDate(LocalDate.of(2026, 3, 28)));
    }

    @Test
    void adminCancellationWindowOpenBeforePreviousDayFourThirtyPm() {
        OrderDeadlineService service = new OrderDeadlineService(fixedClock("2026-03-31T16:29:00+08:00"));

        assertDoesNotThrow(() -> service.ensureAdminCancellationWindowOpen(LocalDate.of(2026, 4, 1)));
    }

    @Test
    void employeeCancellationWindowOpenBeforePreviousDayFourThirtyPm() {
        OrderDeadlineService service = new OrderDeadlineService(fixedClock("2026-03-31T16:29:00+08:00"));

        assertDoesNotThrow(() -> service.ensureEmployeeCancellationWindowOpen(LocalDate.of(2026, 4, 1)));
    }

    @Test
    void adminCancellationWindowClosedAtPreviousDayFourThirtyPm() {
        OrderDeadlineService service = new OrderDeadlineService(fixedClock("2026-03-31T16:30:00+08:00"));

        ApiException exception = assertThrows(
                ApiException.class,
                () -> service.ensureAdminCancellationWindowOpen(LocalDate.of(2026, 4, 1)));

        assertEquals("已超過管理員取消訂餐截止時間", exception.getMessage());
    }

    @Test
    void employeeCancellationWindowClosedAtPreviousDayFourThirtyPm() {
        OrderDeadlineService service = new OrderDeadlineService(fixedClock("2026-03-31T16:30:00+08:00"));

        ApiException exception = assertThrows(
                ApiException.class,
                () -> service.ensureEmployeeCancellationWindowOpen(LocalDate.of(2026, 4, 1)));

        assertEquals("已超過取消訂餐截止時間", exception.getMessage());
    }

    @Test
    void adminOrderDateMustBeTomorrow() {
        OrderDeadlineService service = new OrderDeadlineService(fixedClock("2026-03-31T09:00:00+08:00"));

        assertDoesNotThrow(() -> service.ensureAdminOrderDateIsTomorrow(LocalDate.of(2026, 4, 1)));
        ApiException exception = assertThrows(
                ApiException.class,
                () -> service.ensureAdminOrderDateIsTomorrow(LocalDate.of(2026, 4, 2)));

        assertEquals("僅允許建立隔日訂單", exception.getMessage());
    }

    @Test
    void adminOrderCreationWindowOpenBeforePreviousDayFourThirtyPm() {
        OrderDeadlineService service = new OrderDeadlineService(fixedClock("2026-03-31T16:29:00+08:00"));

        assertDoesNotThrow(() -> service.ensureAdminOrderCreationWindowOpen(LocalDate.of(2026, 4, 1)));
    }

    @Test
    void adminOrderCreationWindowClosedAtPreviousDayFourThirtyPm() {
        OrderDeadlineService service = new OrderDeadlineService(fixedClock("2026-03-31T16:30:00+08:00"));

        ApiException exception = assertThrows(
                ApiException.class,
                () -> service.ensureAdminOrderCreationWindowOpen(LocalDate.of(2026, 4, 1)));

        assertEquals("已超過代訂截止時間", exception.getMessage());
    }

    private Clock fixedClock(String isoOffsetDateTime) {
        return Clock.fixed(OffsetDateTime.parse(isoOffsetDateTime).toInstant(), ZONE_ID);
    }
}
