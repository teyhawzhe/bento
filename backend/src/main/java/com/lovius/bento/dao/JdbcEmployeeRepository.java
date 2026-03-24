package com.lovius.bento.dao;

import com.lovius.bento.model.Employee;
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
public class JdbcEmployeeRepository implements EmployeeRepository {
    private final JdbcTemplate jdbcTemplate;

    public JdbcEmployeeRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<Employee> findAll() {
        return jdbcTemplate.query(
                """
                SELECT e.id, e.department_id, d.name AS department_name, e.username, e.password_hash, e.name, e.email,
                       e.is_admin, e.is_active, e.created_at, e.updated_at
                FROM employees e
                JOIN departments d ON d.id = e.department_id
                ORDER BY e.id
                """,
                this::mapRow);
    }

    @Override
    public Optional<Employee> findById(Long id) {
        return findOne(
                """
                SELECT e.id, e.department_id, d.name AS department_name, e.username, e.password_hash, e.name, e.email,
                       e.is_admin, e.is_active, e.created_at, e.updated_at
                FROM employees e
                JOIN departments d ON d.id = e.department_id
                WHERE e.id = ?
                """,
                id);
    }

    @Override
    public Optional<Employee> findByUsername(String username) {
        return findOne(
                """
                SELECT e.id, e.department_id, d.name AS department_name, e.username, e.password_hash, e.name, e.email,
                       e.is_admin, e.is_active, e.created_at, e.updated_at
                FROM employees e
                JOIN departments d ON d.id = e.department_id
                WHERE LOWER(e.username) = LOWER(?)
                """,
                username);
    }

    @Override
    public Optional<Employee> findByEmail(String email) {
        return findOne(
                """
                SELECT e.id, e.department_id, d.name AS department_name, e.username, e.password_hash, e.name, e.email,
                       e.is_admin, e.is_active, e.created_at, e.updated_at
                FROM employees e
                JOIN departments d ON d.id = e.department_id
                WHERE LOWER(e.email) = LOWER(?)
                """,
                email);
    }

    @Override
    public boolean existsByUsername(String username) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM employees WHERE LOWER(username) = LOWER(?)",
                Integer.class,
                username);
        return count != null && count > 0;
    }

    @Override
    public boolean existsByEmail(String email) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM employees WHERE LOWER(email) = LOWER(?)",
                Integer.class,
                email);
        return count != null && count > 0;
    }

    @Override
    public boolean existsByDepartmentId(Long departmentId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM employees WHERE department_id = ?",
                Integer.class,
                departmentId);
        return count != null && count > 0;
    }

    @Override
    public Employee save(Employee employee) {
        if (employee.getId() == null) {
            return insert(employee);
        }
        return update(employee);
    }

    private Optional<Employee> findOne(String sql, Object parameter) {
        List<Employee> results = jdbcTemplate.query(sql, this::mapRow, parameter);
        return results.stream().findFirst();
    }

    private Employee insert(Employee employee) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var statement = connection.prepareStatement(
                    """
                    INSERT INTO employees (
                        department_id, username, password_hash, name, email, is_admin, is_active, created_at, updated_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, employee.getDepartmentId());
            statement.setString(2, employee.getUsername());
            statement.setString(3, employee.getPasswordHash());
            statement.setString(4, employee.getName());
            statement.setString(5, employee.getEmail());
            statement.setBoolean(6, employee.isAdmin());
            statement.setBoolean(7, employee.isActive());
            statement.setObject(8, employee.getCreatedAt());
            statement.setObject(9, employee.getUpdatedAt());
            return statement;
        }, keyHolder);

        Number key = keyHolder.getKey();
        employee.setId(key == null ? null : key.longValue());
        return employee;
    }

    private Employee update(Employee employee) {
        jdbcTemplate.update(
                """
                UPDATE employees
                SET department_id = ?, username = ?, password_hash = ?, name = ?, email = ?, is_admin = ?, is_active = ?, updated_at = ?
                WHERE id = ?
                """,
                employee.getDepartmentId(),
                employee.getUsername(),
                employee.getPasswordHash(),
                employee.getName(),
                employee.getEmail(),
                employee.isAdmin(),
                employee.isActive(),
                employee.getUpdatedAt(),
                employee.getId());
        return employee;
    }

    private Employee mapRow(ResultSet resultSet, int rowNumber) throws SQLException {
        return new Employee(
                resultSet.getLong("id"),
                resultSet.getLong("department_id"),
                resultSet.getString("department_name"),
                resultSet.getString("username"),
                resultSet.getString("password_hash"),
                resultSet.getString("name"),
                resultSet.getString("email"),
                resultSet.getBoolean("is_admin"),
                resultSet.getBoolean("is_active"),
                resultSet.getTimestamp("created_at").toInstant(),
                resultSet.getTimestamp("updated_at").toInstant());
    }
}
