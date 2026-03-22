package com.lovius.bento.dao;

import com.lovius.bento.model.AdminOrderView;
import com.lovius.bento.model.BentoOrder;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcOrderRepository implements OrderRepository {
    private final JdbcTemplate jdbcTemplate;

    public JdbcOrderRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public BentoOrder save(BentoOrder order) {
        if (order.getId() == null) {
            return insert(order);
        }
        return update(order);
    }

    @Override
    public Optional<BentoOrder> findById(Long id) {
        List<BentoOrder> orders = jdbcTemplate.query(
                """
                SELECT id, employee_id, menu_id, order_date, created_by, created_at
                FROM orders
                WHERE id = ?
                """,
                this::mapRow,
                id);
        return orders.stream().findFirst();
    }

    @Override
    public Optional<BentoOrder> findByEmployeeIdAndOrderDate(Long employeeId, LocalDate orderDate) {
        List<BentoOrder> orders = jdbcTemplate.query(
                """
                SELECT id, employee_id, menu_id, order_date, created_by, created_at
                FROM orders
                WHERE employee_id = ? AND order_date = ?
                """,
                this::mapRow,
                employeeId,
                orderDate);
        return orders.stream().findFirst();
    }

    @Override
    public List<BentoOrder> findByEmployeeId(Long employeeId) {
        return jdbcTemplate.query(
                """
                SELECT id, employee_id, menu_id, order_date, created_by, created_at
                FROM orders
                WHERE employee_id = ?
                ORDER BY order_date DESC, created_at DESC
                """,
                this::mapRow,
                employeeId);
    }

    @Override
    public List<AdminOrderView> findAdminOrders(LocalDate dateFrom, LocalDate dateTo, Long employeeId) {
        StringBuilder sql = new StringBuilder(
                """
                SELECT ord.id,
                       ord.employee_id,
                       employee.name AS employee_name,
                       ord.menu_id,
                       menu.name AS menu_name,
                       supplier.id AS supplier_id,
                       supplier.name AS supplier_name,
                       menu.price AS menu_price,
                       ord.order_date,
                       ord.created_by,
                       creator.name AS created_by_name,
                       ord.created_at
                FROM orders ord
                JOIN employees employee ON employee.id = ord.employee_id
                JOIN menus menu ON menu.id = ord.menu_id
                JOIN suppliers supplier ON supplier.id = menu.supplier_id
                LEFT JOIN employees creator ON creator.id = ord.created_by
                WHERE 1 = 1
                """);
        List<Object> parameters = new ArrayList<>();
        if (dateFrom != null) {
            sql.append(" AND ord.order_date >= ?");
            parameters.add(dateFrom);
        }
        if (dateTo != null) {
            sql.append(" AND ord.order_date <= ?");
            parameters.add(dateTo);
        }
        if (employeeId != null) {
            sql.append(" AND ord.employee_id = ?");
            parameters.add(employeeId);
        }
        sql.append(" ORDER BY ord.order_date DESC, ord.created_at DESC, ord.id DESC");
        return jdbcTemplate.query(sql.toString(), this::mapAdminRow, parameters.toArray());
    }

    @Override
    public void deleteById(Long id) {
        jdbcTemplate.update("DELETE FROM orders WHERE id = ?", id);
    }

    private BentoOrder insert(BentoOrder order) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var statement = connection.prepareStatement(
                    """
                    INSERT INTO orders (employee_id, menu_id, order_date, created_by, created_at)
                    VALUES (?, ?, ?, ?, ?)
                    """,
                    Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, order.getEmployeeId());
            statement.setLong(2, order.getMenuId());
            statement.setObject(3, order.getOrderDate());
            statement.setObject(4, order.getCreatedBy());
            statement.setObject(5, order.getCreatedAt());
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        order.setId(key == null ? null : key.longValue());
        return order;
    }

    private BentoOrder update(BentoOrder order) {
        jdbcTemplate.update(
                """
                UPDATE orders
                SET menu_id = ?, created_by = ?, created_at = ?
                WHERE id = ?
                """,
                order.getMenuId(),
                order.getCreatedBy(),
                order.getCreatedAt(),
                order.getId());
        return order;
    }

    private BentoOrder mapRow(ResultSet resultSet, int rowNumber) throws SQLException {
        return new BentoOrder(
                resultSet.getLong("id"),
                resultSet.getLong("employee_id"),
                resultSet.getLong("menu_id"),
                resultSet.getDate("order_date").toLocalDate(),
                resultSet.getObject("created_by", Long.class),
                resultSet.getTimestamp("created_at").toInstant());
    }

    private AdminOrderView mapAdminRow(ResultSet resultSet, int rowNumber) throws SQLException {
        BigDecimal price = resultSet.getBigDecimal("menu_price");
        return new AdminOrderView(
                resultSet.getLong("id"),
                resultSet.getLong("employee_id"),
                resultSet.getString("employee_name"),
                resultSet.getLong("menu_id"),
                resultSet.getString("menu_name"),
                resultSet.getLong("supplier_id"),
                resultSet.getString("supplier_name"),
                price,
                resultSet.getDate("order_date").toLocalDate(),
                resultSet.getObject("created_by", Long.class),
                resultSet.getString("created_by_name"),
                resultSet.getTimestamp("created_at").toInstant());
    }
}
