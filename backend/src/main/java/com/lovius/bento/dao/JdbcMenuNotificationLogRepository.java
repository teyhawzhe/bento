package com.lovius.bento.dao;

import com.lovius.bento.model.MenuNotificationLog;
import java.sql.Statement;
import java.time.LocalDate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcMenuNotificationLogRepository implements MenuNotificationLogRepository {
    private final JdbcTemplate jdbcTemplate;

    public JdbcMenuNotificationLogRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public boolean existsByNotifyDate(LocalDate notifyDate) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM menu_notification_log WHERE notify_date = ?",
                Integer.class,
                notifyDate);
        return count != null && count > 0;
    }

    @Override
    public MenuNotificationLog save(MenuNotificationLog log) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var statement = connection.prepareStatement(
                    """
                    INSERT INTO menu_notification_log (
                        notify_date,
                        missing_from,
                        missing_to,
                        status,
                        created_at
                    ) VALUES (?, ?, ?, ?, ?)
                    """,
                    Statement.RETURN_GENERATED_KEYS);
            statement.setObject(1, log.getNotifyDate());
            statement.setObject(2, log.getMissingFrom());
            statement.setObject(3, log.getMissingTo());
            statement.setString(4, log.getStatus());
            statement.setObject(5, log.getCreatedAt());
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        log.setId(key == null ? null : key.longValue());
        return log;
    }
}
