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

    public List<LocalDate> nextWeekdays() {
        LocalDate today = ZonedDateTime.now(clock).toLocalDate();
        LocalDate nextMonday = today.with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        List<LocalDate> weekdays = new ArrayList<>();
        for (int offset = 0; offset < 5; offset++) {
            weekdays.add(nextMonday.plusDays(offset));
        }
        return weekdays;
    }

    public boolean isOrderDateWithinNextWeekdays(LocalDate orderDate) {
        return nextWeekdays().contains(orderDate);
    }

    public void ensureEmployeeOrderWindowOpen() {
        ZonedDateTime now = ZonedDateTime.now(clock);
        LocalDate friday = now.toLocalDate().with(TemporalAdjusters.nextOrSame(DayOfWeek.FRIDAY));
        ZonedDateTime deadline = ZonedDateTime.of(friday, LocalTime.NOON, ZONE_ID);
        if (!now.isBefore(deadline)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "已超過訂餐截止時間");
        }
    }

    public boolean isEmployeeOrderWindowOpen() {
        try {
            ensureEmployeeOrderWindowOpen();
            return true;
        } catch (ApiException exception) {
            return false;
        }
    }

    public void ensureAdminCancellationWindowOpen(LocalDate orderDate) {
        ZonedDateTime now = ZonedDateTime.now(clock);
        LocalDateTime cutoffDateTime = LocalDateTime.of(orderDate.minusDays(1), LocalTime.of(17, 0));
        ZonedDateTime cutoff = ZonedDateTime.of(cutoffDateTime, ZONE_ID);
        if (!now.isBefore(cutoff)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "已超過管理員取消訂餐截止時間");
        }
    }
}
