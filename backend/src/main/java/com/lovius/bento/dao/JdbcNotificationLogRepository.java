package com.lovius.bento.dao;

import com.lovius.bento.model.NotificationLog;
import java.sql.Statement;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcNotificationLogRepository implements NotificationLogRepository {
    private final JdbcTemplate jdbcTemplate;

    public JdbcNotificationLogRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public NotificationLog save(NotificationLog log) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var statement = connection.prepareStatement(
                    """
                    INSERT INTO notification_logs (
                        notify_date,
                        email_to,
                        content,
                        status,
                        error_message,
                        created_at
                    ) VALUES (?, ?, ?, ?, ?, ?)
                    """,
                    Statement.RETURN_GENERATED_KEYS);
            statement.setObject(1, log.getNotifyDate());
            statement.setString(2, log.getEmailTo());
            statement.setString(3, log.getContent());
            statement.setString(4, log.getStatus());
            statement.setString(5, log.getErrorMessage());
            statement.setObject(6, log.getCreatedAt());
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        log.setId(key == null ? null : key.longValue());
        return log;
    }
}
