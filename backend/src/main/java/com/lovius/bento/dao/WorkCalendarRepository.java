package com.lovius.bento.dao;

import com.lovius.bento.model.WorkCalendar;
import java.time.LocalDate;
import java.util.List;

public interface WorkCalendarRepository {
    List<WorkCalendar> findByDateRange(LocalDate startDate, LocalDate endDate);

    void saveAll(List<WorkCalendar> calendars);

    void deleteByYear(int year);
}
