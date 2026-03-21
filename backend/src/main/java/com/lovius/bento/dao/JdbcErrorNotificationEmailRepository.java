package com.lovius.bento.dao;

import com.lovius.bento.model.ErrorNotificationEmail;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcErrorNotificationEmailRepository implements ErrorNotificationEmailRepository {
    private final JdbcTemplate jdbcTemplate;

    public JdbcErrorNotificationEmailRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<ErrorNotificationEmail> findAll() {
        return jdbcTemplate.query(
                """
                SELECT id, email, created_by, created_at
                FROM error_notification_emails
                ORDER BY id
                """,
                this::mapRow);
    }

    @Override
    public Optional<ErrorNotificationEmail> findById(Long id) {
        List<ErrorNotificationEmail> results = jdbcTemplate.query(
                """
                SELECT id, email, created_by, created_at
                FROM error_notification_emails
                WHERE id = ?
                """,
                this::mapRow,
                id);
        return results.stream().findFirst();
    }

    @Override
    public Optional<ErrorNotificationEmail> findByEmail(String email) {
        List<ErrorNotificationEmail> results = jdbcTemplate.query(
                """
                SELECT id, email, created_by, created_at
                FROM error_notification_emails
                WHERE LOWER(email) = LOWER(?)
                """,
                this::mapRow,
                email);
        return results.stream().findFirst();
    }

    @Override
    public boolean existsByEmail(String email) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM error_notification_emails WHERE LOWER(email) = LOWER(?)",
                Integer.class,
                email);
        return count != null && count > 0;
    }

    @Override
    public ErrorNotificationEmail save(ErrorNotificationEmail errorNotificationEmail) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var statement = connection.prepareStatement(
                    """
                    INSERT INTO error_notification_emails (email, created_by, created_at)
                    VALUES (?, ?, ?)
                    """,
                    Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, errorNotificationEmail.getEmail());
            statement.setLong(2, errorNotificationEmail.getCreatedBy());
            statement.setObject(3, errorNotificationEmail.getCreatedAt());
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        errorNotificationEmail.setId(key == null ? null : key.longValue());
        return errorNotificationEmail;
    }

    @Override
    public void deleteById(Long id) {
        jdbcTemplate.update("DELETE FROM error_notification_emails WHERE id = ?", id);
    }

    private ErrorNotificationEmail mapRow(ResultSet resultSet, int rowNumber) throws SQLException {
        return new ErrorNotificationEmail(
                resultSet.getLong("id"),
                resultSet.getString("email"),
                resultSet.getLong("created_by"),
                resultSet.getTimestamp("created_at").toInstant());
    }
}
