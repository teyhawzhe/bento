package com.lovius.bento.dao;

import com.lovius.bento.model.WorkCalendar;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcWorkCalendarRepository implements WorkCalendarRepository {
    private final JdbcTemplate jdbcTemplate;

    public JdbcWorkCalendarRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<WorkCalendar> findByDateRange(LocalDate startDate, LocalDate endDate) {
        return jdbcTemplate.query(
                """
                SELECT date, is_workday
                FROM work_calendar
                WHERE date BETWEEN ? AND ?
                ORDER BY date ASC
                """,
                this::mapRow,
                startDate,
                endDate);
    }

    @Override
    public void saveAll(List<WorkCalendar> calendars) {
        jdbcTemplate.batchUpdate(
                """
                INSERT INTO work_calendar (date, is_workday)
                VALUES (?, ?)
                ON DUPLICATE KEY UPDATE is_workday = VALUES(is_workday)
                """,
                calendars,
                calendars.size(),
                (statement, calendar) -> {
                    statement.setDate(1, Date.valueOf(calendar.getDate()));
                    statement.setBoolean(2, calendar.isWorkday());
                });
    }

    @Override
    public void deleteByYear(int year) {
        jdbcTemplate.update(
                """
                DELETE FROM work_calendar
                WHERE YEAR(date) = ?
                """,
                year);
    }

    private WorkCalendar mapRow(ResultSet resultSet, int rowNumber) throws SQLException {
        return new WorkCalendar(
                resultSet.getDate("date").toLocalDate(),
                resultSet.getBoolean("is_workday"));
    }
}
