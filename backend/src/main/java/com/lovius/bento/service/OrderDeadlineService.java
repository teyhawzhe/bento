package com.lovius.bento.service;

import com.lovius.bento.exception.ApiException;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class OrderDeadlineService {
    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Taipei");
    private final Clock clock;

    public OrderDeadlineService() {
        this(Clock.system(ZONE_ID));
    }

    OrderDeadlineService(Clock clock) {
        this.clock = clock;
    }

    public List<LocalDate> employeeOrderableDates() {
        LocalDate rangeStart = employeeOrderRangeStart();
        LocalDate rangeEnd = employeeOrderRangeEnd();
        List<LocalDate> orderableDates = new ArrayList<>();
        for (LocalDate date = rangeStart; !date.isAfter(rangeEnd); date = date.plusDays(1)) {
            orderableDates.add(date);
        }
        return orderableDates;
    }

    public void ensureEmployeeOrderableDate(LocalDate orderDate) {
        LocalDate rangeStart = employeeOrderRangeStart();
        LocalDate rangeEnd = employeeOrderRangeEnd();
        if (orderDate.isBefore(rangeStart) || orderDate.isAfter(rangeEnd)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "僅可訂購本次開放區間（截止日後至下週五）的便當");
        }
    }

    public void ensureAdminCancellationWindowOpen(LocalDate orderDate) {
        ensureCancellationWindowOpen(orderDate, "已超過管理員取消訂餐截止時間");
    }

    public void ensureEmployeeCancellationWindowOpen(LocalDate orderDate) {
        ensureCancellationWindowOpen(orderDate, "已超過取消訂餐截止時間");
    }

    public void ensureAdminOrderDateIsTomorrow(LocalDate orderDate) {
        LocalDate tomorrow = ZonedDateTime.now(clock).toLocalDate().plusDays(1);
        if (!tomorrow.equals(orderDate)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "僅允許建立隔日訂單");
        }
    }

    public void ensureAdminOrderCreationWindowOpen(LocalDate orderDate) {
        ensureAdminWindowOpen(orderDate, "已超過代訂截止時間");
    }

    public ZonedDateTime employeeOrderDeadline() {
        ZonedDateTime now = now();
        LocalDate friday = now.toLocalDate().with(TemporalAdjusters.nextOrSame(DayOfWeek.FRIDAY));
        ZonedDateTime deadline = ZonedDateTime.of(friday, LocalTime.NOON, ZONE_ID);
        if (!now.isBefore(deadline)) {
            return deadline.plusWeeks(1);
        }
        return deadline;
    }

    public LocalDate employeeOrderRangeStart() {
        return employeeOrderDeadline().toLocalDate().plusDays(1);
    }

    public LocalDate employeeOrderRangeEnd() {
        return employeeOrderDeadline().toLocalDate().plusDays(7);
    }

    private ZonedDateTime now() {
        return ZonedDateTime.now(clock);
    }

    private void ensureCancellationWindowOpen(LocalDate orderDate, String message) {
        ZonedDateTime now = now();
        LocalDateTime cutoffDateTime = LocalDateTime.of(orderDate.minusDays(1), LocalTime.of(16, 30));
        ZonedDateTime cutoff = ZonedDateTime.of(cutoffDateTime, ZONE_ID);
        if (!now.isBefore(cutoff)) {
            throw new ApiException(HttpStatus.FORBIDDEN, message);
        }
    }

    private void ensureAdminWindowOpen(LocalDate orderDate, String message) {
        ZonedDateTime now = now();
        LocalDateTime cutoffDateTime = LocalDateTime.of(orderDate.minusDays(1), LocalTime.of(17, 0));
        ZonedDateTime cutoff = ZonedDateTime.of(cutoffDateTime, ZONE_ID);
        if (!now.isBefore(cutoff)) {
            throw new ApiException(HttpStatus.FORBIDDEN, message);
        }
    }
}
