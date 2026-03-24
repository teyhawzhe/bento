package com.lovius.bento.config;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class SchemaMigrationInitializer {
    private static final Logger logger = LoggerFactory.getLogger(SchemaMigrationInitializer.class);
    private static final String DEFAULT_DEPARTMENT_NAME = "未指定部門";

    @Bean
    public ApplicationRunner migrateDepartmentSchema(JdbcTemplate jdbcTemplate) {
        return arguments -> {
            logger.info("Starting department schema migration");
            ensureDepartmentsTable(jdbcTemplate);
            ensureEmployeeDepartmentColumn(jdbcTemplate);
            logger.info("Department schema migration finished");
        };
    }

    private void ensureDepartmentsTable(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.execute(
                """
                CREATE TABLE IF NOT EXISTS departments (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    name VARCHAR(100) NOT NULL,
                    is_active BOOLEAN NOT NULL DEFAULT TRUE,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_departments_name (name)
                )
                """);
        logger.info("Ensured departments table exists");
    }

    private void ensureEmployeeDepartmentColumn(JdbcTemplate jdbcTemplate) {
        if (!tableExists(jdbcTemplate, "employees")) {
            logger.info("Skipped employee department migration because employees table does not exist");
            return;
        }

        Long defaultDepartmentId = getOrCreateDefaultDepartment(jdbcTemplate);

        if (!columnExists(jdbcTemplate, "employees", "department_id")) {
            logger.info("Adding employees.department_id column");
            jdbcTemplate.execute("ALTER TABLE employees ADD COLUMN department_id BIGINT NULL AFTER id");
        }

        int updatedRows = jdbcTemplate.update(
                "UPDATE employees SET department_id = ? WHERE department_id IS NULL",
                defaultDepartmentId);
        logger.info("Backfilled department_id for {} employee rows", updatedRows);

        jdbcTemplate.execute("ALTER TABLE employees MODIFY COLUMN department_id BIGINT NOT NULL");
        logger.info("Ensured employees.department_id is NOT NULL");

        if (!indexExists(jdbcTemplate, "employees", "idx_employees_department_id")) {
            logger.info("Adding idx_employees_department_id");
            jdbcTemplate.execute("ALTER TABLE employees ADD INDEX idx_employees_department_id (department_id)");
        }

        if (!foreignKeyExists(jdbcTemplate, "employees", "fk_employees_department")) {
            logger.info("Adding fk_employees_department");
            jdbcTemplate.execute(
                    """
                    ALTER TABLE employees
                    ADD CONSTRAINT fk_employees_department
                    FOREIGN KEY (department_id) REFERENCES departments(id)
                    """);
        }
    }

    private Long getOrCreateDefaultDepartment(JdbcTemplate jdbcTemplate) {
        List<Long> existingIds = jdbcTemplate.query(
                "SELECT id FROM departments WHERE LOWER(name) = LOWER(?)",
                (resultSet, rowNum) -> resultSet.getLong("id"),
                DEFAULT_DEPARTMENT_NAME);
        if (!existingIds.isEmpty()) {
            logger.info("Using existing default department id={}", existingIds.getFirst());
            return existingIds.getFirst();
        }

        jdbcTemplate.update(
                """
                INSERT INTO departments (name, is_active, created_at, updated_at)
                VALUES (?, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """,
                DEFAULT_DEPARTMENT_NAME);

        Long departmentId = jdbcTemplate.queryForObject(
                "SELECT id FROM departments WHERE LOWER(name) = LOWER(?)",
                Long.class,
                DEFAULT_DEPARTMENT_NAME);
        logger.info("Created default department id={}", departmentId);
        return departmentId;
    }

    private boolean tableExists(JdbcTemplate jdbcTemplate, String tableName) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = DATABASE() AND table_name = ?
                """,
                Integer.class,
                tableName);
        return count != null && count > 0;
    }

    private boolean columnExists(JdbcTemplate jdbcTemplate, String tableName, String columnName) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?
                """,
                Integer.class,
                tableName,
                columnName);
        return count != null && count > 0;
    }

    private boolean indexExists(JdbcTemplate jdbcTemplate, String tableName, String indexName) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.statistics
                WHERE table_schema = DATABASE() AND table_name = ? AND index_name = ?
                """,
                Integer.class,
                tableName,
                indexName);
        return count != null && count > 0;
    }

    private boolean foreignKeyExists(JdbcTemplate jdbcTemplate, String tableName, String constraintName) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.table_constraints
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                  AND constraint_name = ?
                  AND constraint_type = 'FOREIGN KEY'
                """,
                Integer.class,
                tableName,
                constraintName);
        return count != null && count > 0;
    }
}
