package com.lovius.bento.dao;

import com.lovius.bento.model.ReportRecipientEmail;
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
public class JdbcReportRecipientEmailRepository implements ReportRecipientEmailRepository {
    private final JdbcTemplate jdbcTemplate;

    public JdbcReportRecipientEmailRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<ReportRecipientEmail> findAll() {
        return jdbcTemplate.query(
                """
                SELECT id, email, created_by, created_at
                FROM report_recipient_emails
                ORDER BY id
                """,
                this::mapRow);
    }

    @Override
    public Optional<ReportRecipientEmail> findById(Long id) {
        List<ReportRecipientEmail> results = jdbcTemplate.query(
                """
                SELECT id, email, created_by, created_at
                FROM report_recipient_emails
                WHERE id = ?
                """,
                this::mapRow,
                id);
        return results.stream().findFirst();
    }

    @Override
    public Optional<ReportRecipientEmail> findByEmail(String email) {
        List<ReportRecipientEmail> results = jdbcTemplate.query(
                """
                SELECT id, email, created_by, created_at
                FROM report_recipient_emails
                WHERE LOWER(email) = LOWER(?)
                """,
                this::mapRow,
                email);
        return results.stream().findFirst();
    }

    @Override
    public boolean existsByEmail(String email) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM report_recipient_emails WHERE LOWER(email) = LOWER(?)",
                Integer.class,
                email);
        return count != null && count > 0;
    }

    @Override
    public ReportRecipientEmail save(ReportRecipientEmail reportRecipientEmail) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var statement = connection.prepareStatement(
                    """
                    INSERT INTO report_recipient_emails (email, created_by, created_at)
                    VALUES (?, ?, ?)
                    """,
                    Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, reportRecipientEmail.getEmail());
            statement.setLong(2, reportRecipientEmail.getCreatedBy());
            statement.setObject(3, reportRecipientEmail.getCreatedAt());
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        reportRecipientEmail.setId(key == null ? null : key.longValue());
        return reportRecipientEmail;
    }

    @Override
    public void deleteById(Long id) {
        jdbcTemplate.update("DELETE FROM report_recipient_emails WHERE id = ?", id);
    }

    private ReportRecipientEmail mapRow(ResultSet resultSet, int rowNumber) throws SQLException {
        return new ReportRecipientEmail(
                resultSet.getLong("id"),
                resultSet.getString("email"),
                resultSet.getLong("created_by"),
                resultSet.getTimestamp("created_at").toInstant());
    }
}
