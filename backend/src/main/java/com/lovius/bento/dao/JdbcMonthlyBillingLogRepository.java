package com.lovius.bento.dao;

import com.lovius.bento.model.MonthlyBillingLog;
import com.lovius.bento.model.MonthlyBillingLogView;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcMonthlyBillingLogRepository implements MonthlyBillingLogRepository {
    private final JdbcTemplate jdbcTemplate;

    public JdbcMonthlyBillingLogRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public MonthlyBillingLog save(MonthlyBillingLog log) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var statement = connection.prepareStatement(
                    """
                    INSERT INTO monthly_billing_logs (
                        billing_period_start,
                        billing_period_end,
                        supplier_id,
                        email_to,
                        status,
                        error_message,
                        triggered_by,
                        sent_at,
                        created_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    Statement.RETURN_GENERATED_KEYS);
            statement.setObject(1, log.getBillingPeriodStart());
            statement.setObject(2, log.getBillingPeriodEnd());
            statement.setLong(3, log.getSupplierId());
            statement.setString(4, log.getEmailTo());
            statement.setString(5, log.getStatus());
            statement.setString(6, log.getErrorMessage());
            if (log.getTriggeredBy() == null) {
                statement.setObject(7, null);
            } else {
                statement.setLong(7, log.getTriggeredBy());
            }
            statement.setObject(8, log.getSentAt());
            statement.setObject(9, log.getCreatedAt());
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        log.setId(key == null ? null : key.longValue());
        return log;
    }

    @Override
    public List<MonthlyBillingLogView> findAll() {
        return jdbcTemplate.query(
                """
                SELECT
                    log.id,
                    log.billing_period_start,
                    log.billing_period_end,
                    log.supplier_id,
                    supplier.name AS supplier_name,
                    log.email_to,
                    log.status,
                    log.error_message,
                    log.triggered_by,
                    log.sent_at,
                    log.created_at
                FROM monthly_billing_logs log
                JOIN suppliers supplier ON supplier.id = log.supplier_id
                ORDER BY log.created_at DESC, log.id DESC
                """,
                this::mapViewRow);
    }

    private MonthlyBillingLogView mapViewRow(ResultSet resultSet, int rowNumber) throws SQLException {
        return new MonthlyBillingLogView(
                resultSet.getLong("id"),
                resultSet.getDate("billing_period_start").toLocalDate(),
                resultSet.getDate("billing_period_end").toLocalDate(),
                resultSet.getLong("supplier_id"),
                resultSet.getString("supplier_name"),
                resultSet.getString("email_to"),
                resultSet.getString("status"),
                resultSet.getString("error_message"),
                resultSet.getObject("triggered_by") == null ? null : resultSet.getLong("triggered_by"),
                resultSet.getTimestamp("sent_at") == null ? null : resultSet.getTimestamp("sent_at").toInstant(),
                resultSet.getTimestamp("created_at").toInstant());
    }
}
