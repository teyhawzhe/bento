package com.lovius.bento.dao;

import com.lovius.bento.model.Supplier;
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
public class JdbcSupplierRepository implements SupplierRepository {
    private final JdbcTemplate jdbcTemplate;

    public JdbcSupplierRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Supplier save(Supplier supplier) {
        if (supplier.getId() != null) {
            return update(supplier);
        }
        return insert(supplier);
    }

    private Supplier insert(Supplier supplier) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var statement = connection.prepareStatement(
                    """
                    INSERT INTO suppliers (
                        name, email, phone, contact_person, business_registration_no, is_active, created_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?)
                    """,
                    Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, supplier.getName());
            statement.setString(2, supplier.getEmail());
            statement.setString(3, supplier.getPhone());
            statement.setString(4, supplier.getContactPerson());
            statement.setString(5, supplier.getBusinessRegistrationNo());
            statement.setBoolean(6, supplier.isActive());
            statement.setObject(7, supplier.getCreatedAt());
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        supplier.setId(key == null ? null : key.longValue());
        return supplier;
    }

    @Override
    public Optional<Supplier> findById(Long id) {
        List<Supplier> suppliers = jdbcTemplate.query(
                """
                SELECT id, name, email, phone, contact_person, business_registration_no, is_active, created_at
                FROM suppliers
                WHERE id = ?
                """,
                this::mapRow,
                id);
        return suppliers.stream().findFirst();
    }

    @Override
    public List<Supplier> findAll() {
        return jdbcTemplate.query(
                """
                SELECT id, name, email, phone, contact_person, business_registration_no, is_active, created_at
                FROM suppliers
                ORDER BY id
                """,
                this::mapRow);
    }

    @Override
    public List<Supplier> findByNameExact(String name) {
        return jdbcTemplate.query(
                """
                SELECT id, name, email, phone, contact_person, business_registration_no, is_active, created_at
                FROM suppliers
                WHERE LOWER(name) = LOWER(?)
                ORDER BY id
                """,
                this::mapRow,
                name);
    }

    @Override
    public List<Supplier> findByNameFuzzy(String keyword) {
        return jdbcTemplate.query(
                """
                SELECT id, name, email, phone, contact_person, business_registration_no, is_active, created_at
                FROM suppliers
                WHERE LOWER(name) LIKE LOWER(?)
                ORDER BY id
                """,
                this::mapRow,
                "%" + keyword + "%");
    }

    private Supplier update(Supplier supplier) {
        jdbcTemplate.update(
                """
                UPDATE suppliers
                SET name = ?, email = ?, phone = ?, contact_person = ?, is_active = ?
                WHERE id = ?
                """,
                supplier.getName(),
                supplier.getEmail(),
                supplier.getPhone(),
                supplier.getContactPerson(),
                supplier.isActive(),
                supplier.getId());
        return supplier;
    }

    private Supplier mapRow(ResultSet resultSet, int rowNumber) throws SQLException {
        return new Supplier(
                resultSet.getLong("id"),
                resultSet.getString("name"),
                resultSet.getString("email"),
                resultSet.getString("phone"),
                resultSet.getString("contact_person"),
                resultSet.getString("business_registration_no"),
                resultSet.getBoolean("is_active"),
                resultSet.getTimestamp("created_at").toInstant());
    }
}
