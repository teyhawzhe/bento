package com.lovius.bento.dao;

import com.lovius.bento.model.MonthlyBillingAggregationRow;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcMonthlyBillingReportRepository implements MonthlyBillingReportRepository {
    private final JdbcTemplate jdbcTemplate;

    public JdbcMonthlyBillingReportRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<MonthlyBillingAggregationRow> findBillingRows(LocalDate periodStart, LocalDate periodEnd) {
        return jdbcTemplate.query(
                """
                SELECT
                    supplier.id AS supplier_id,
                    supplier.name AS supplier_name,
                    supplier.email AS supplier_email,
                    menu.name AS menu_name,
                    menu.price AS unit_price,
                    COUNT(*) AS quantity
                FROM orders ord
                JOIN menus menu ON menu.id = ord.menu_id
                JOIN suppliers supplier ON supplier.id = menu.supplier_id
                WHERE ord.order_date BETWEEN ? AND ?
                GROUP BY supplier.id, supplier.name, supplier.email, menu.name, menu.price
                ORDER BY supplier.id ASC, menu.name ASC
                """,
                this::mapRow,
                periodStart,
                periodEnd);
    }

    private MonthlyBillingAggregationRow mapRow(ResultSet resultSet, int rowNumber) throws SQLException {
        return new MonthlyBillingAggregationRow(
                resultSet.getLong("supplier_id"),
                resultSet.getString("supplier_name"),
                resultSet.getString("supplier_email"),
                resultSet.getString("menu_name"),
                resultSet.getBigDecimal("unit_price").setScale(2, RoundingMode.HALF_UP),
                resultSet.getLong("quantity"));
    }
}
