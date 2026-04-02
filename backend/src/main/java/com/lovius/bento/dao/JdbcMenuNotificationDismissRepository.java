package com.lovius.bento.dao;

import com.lovius.bento.model.MenuNotificationDismiss;
import java.time.LocalDate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcMenuNotificationDismissRepository implements MenuNotificationDismissRepository {
    private final JdbcTemplate jdbcTemplate;

    public JdbcMenuNotificationDismissRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public boolean existsByDismissDate(LocalDate dismissDate) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM menu_notification_dismiss WHERE dismiss_date = ?",
                Integer.class,
                dismissDate);
        return count != null && count > 0;
    }

    @Override
    public MenuNotificationDismiss save(MenuNotificationDismiss dismiss) {
        jdbcTemplate.update(
                """
                INSERT INTO menu_notification_dismiss (dismiss_date, created_at)
                VALUES (?, ?)
                ON DUPLICATE KEY UPDATE created_at = VALUES(created_at)
                """,
                dismiss.getDismissDate(),
                dismiss.getCreatedAt());
        return dismiss;
    }
}
