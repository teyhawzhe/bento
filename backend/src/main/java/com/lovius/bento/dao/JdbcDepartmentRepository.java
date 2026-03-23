package com.lovius.bento.dao;

import com.lovius.bento.model.Department;
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
public class JdbcDepartmentRepository implements DepartmentRepository {
    private final JdbcTemplate jdbcTemplate;

    public JdbcDepartmentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<Department> findAll() {
        return jdbcTemplate.query(
                """
                SELECT id, name, is_active, created_at, updated_at
                FROM departments
                ORDER BY is_active DESC, name ASC, id ASC
                """,
                this::mapRow);
    }

    @Override
    public Optional<Department> findById(Long id) {
        return findOne(
                """
                SELECT id, name, is_active, created_at, updated_at
                FROM departments
                WHERE id = ?
                """,
                id);
    }

    @Override
    public Optional<Department> findByName(String name) {
        return findOne(
                """
                SELECT id, name, is_active, created_at, updated_at
                FROM departments
                WHERE LOWER(name) = LOWER(?)
                """,
                name);
    }

    @Override
    public Department save(Department department) {
        if (department.getId() == null) {
            return insert(department);
        }
        return update(department);
    }

    private Optional<Department> findOne(String sql, Object parameter) {
        List<Department> results = jdbcTemplate.query(sql, this::mapRow, parameter);
        return results.stream().findFirst();
    }

    private Department insert(Department department) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var statement = connection.prepareStatement(
                    """
                    INSERT INTO departments (name, is_active, created_at, updated_at)
                    VALUES (?, ?, ?, ?)
                    """,
                    Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, department.getName());
            statement.setBoolean(2, department.isActive());
            statement.setObject(3, department.getCreatedAt());
            statement.setObject(4, department.getUpdatedAt());
            return statement;
        }, keyHolder);

        Number key = keyHolder.getKey();
        department.setId(key == null ? null : key.longValue());
        return department;
    }

    private Department update(Department department) {
        jdbcTemplate.update(
                """
                UPDATE departments
                SET name = ?, is_active = ?, updated_at = ?
                WHERE id = ?
                """,
                department.getName(),
                department.isActive(),
                department.getUpdatedAt(),
                department.getId());
        return department;
    }

    private Department mapRow(ResultSet resultSet, int rowNumber) throws SQLException {
        return new Department(
                resultSet.getLong("id"),
                resultSet.getString("name"),
                resultSet.getBoolean("is_active"),
                resultSet.getTimestamp("created_at").toInstant(),
                resultSet.getTimestamp("updated_at").toInstant());
    }
}
