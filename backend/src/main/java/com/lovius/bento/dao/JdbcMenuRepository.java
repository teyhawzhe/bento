package com.lovius.bento.dao;

import com.lovius.bento.model.Menu;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcMenuRepository implements MenuRepository {
    private final JdbcTemplate jdbcTemplate;

    public JdbcMenuRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Menu save(Menu menu) {
        if (menu.getId() == null) {
            return insert(menu);
        }
        return update(menu);
    }

    @Override
    public Optional<Menu> findById(Long id) {
        List<Menu> menus = jdbcTemplate.query(
                """
                SELECT id, supplier_id, name, category, description, price, valid_from, valid_to,
                       created_by, created_at, updated_at
                FROM menus
                WHERE id = ?
                """,
                this::mapRow,
                id);
        return menus.stream().findFirst();
    }

    @Override
    public List<Menu> findAll(boolean includeHistory, LocalDate today, Long supplierId) {
        if (includeHistory) {
            if (supplierId != null) {
                return jdbcTemplate.query(
                        """
                        SELECT id, supplier_id, name, category, description, price, valid_from, valid_to,
                               created_by, created_at, updated_at
                        FROM menus
                        WHERE supplier_id = ?
                        ORDER BY valid_from DESC, id DESC
                        """,
                        this::mapRow,
                        supplierId);
            }
            return jdbcTemplate.query(
                    """
                    SELECT id, supplier_id, name, category, description, price, valid_from, valid_to,
                           created_by, created_at, updated_at
                    FROM menus
                    ORDER BY valid_from DESC, id DESC
                    """,
                    this::mapRow);
        }
        if (supplierId != null) {
            return jdbcTemplate.query(
                    """
                    SELECT id, supplier_id, name, category, description, price, valid_from, valid_to,
                           created_by, created_at, updated_at
                    FROM menus
                    WHERE valid_to >= ? AND supplier_id = ?
                    ORDER BY valid_from ASC, id ASC
                    """,
                    this::mapRow,
                    today,
                    supplierId);
        }
        return jdbcTemplate.query(
                """
                SELECT id, supplier_id, name, category, description, price, valid_from, valid_to,
                       created_by, created_at, updated_at
                FROM menus
                WHERE valid_to >= ?
                ORDER BY valid_from ASC, id ASC
                """,
                this::mapRow,
                today);
    }

    @Override
    public List<Menu> findAvailableForDate(LocalDate orderDate) {
        return jdbcTemplate.query(
                """
                SELECT id, supplier_id, name, category, description, price, valid_from, valid_to,
                       created_by, created_at, updated_at
                FROM menus
                WHERE valid_from <= ? AND valid_to >= ?
                ORDER BY name ASC, id ASC
                """,
                this::mapRow,
                orderDate,
                orderDate);
    }

    private Menu insert(Menu menu) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var statement = connection.prepareStatement(
                    """
                    INSERT INTO menus (
                        supplier_id, name, category, description, price, valid_from, valid_to,
                        created_by, created_at, updated_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, menu.getSupplierId());
            statement.setString(2, menu.getName());
            statement.setString(3, menu.getCategory());
            statement.setString(4, menu.getDescription());
            statement.setBigDecimal(5, menu.getPrice());
            statement.setObject(6, menu.getValidFrom());
            statement.setObject(7, menu.getValidTo());
            statement.setLong(8, menu.getCreatedBy());
            statement.setObject(9, menu.getCreatedAt());
            statement.setObject(10, menu.getUpdatedAt());
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        menu.setId(key == null ? null : key.longValue());
        return menu;
    }

    private Menu update(Menu menu) {
        jdbcTemplate.update(
                """
                UPDATE menus
                SET supplier_id = ?, name = ?, category = ?, description = ?, price = ?,
                    valid_from = ?, valid_to = ?, updated_at = ?
                WHERE id = ?
                """,
                menu.getSupplierId(),
                menu.getName(),
                menu.getCategory(),
                menu.getDescription(),
                menu.getPrice(),
                menu.getValidFrom(),
                menu.getValidTo(),
                menu.getUpdatedAt(),
                menu.getId());
        return menu;
    }

    private Menu mapRow(ResultSet resultSet, int rowNumber) throws SQLException {
        return new Menu(
                resultSet.getLong("id"),
                resultSet.getLong("supplier_id"),
                resultSet.getString("name"),
                resultSet.getString("category"),
                resultSet.getString("description"),
                resultSet.getBigDecimal("price"),
                resultSet.getDate("valid_from").toLocalDate(),
                resultSet.getDate("valid_to").toLocalDate(),
                resultSet.getLong("created_by"),
                resultSet.getTimestamp("created_at").toInstant(),
                resultSet.getTimestamp("updated_at").toInstant());
    }
}
