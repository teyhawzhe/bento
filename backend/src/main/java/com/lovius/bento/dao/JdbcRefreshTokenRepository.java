package com.lovius.bento.dao;

import com.lovius.bento.model.RefreshToken;
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
public class JdbcRefreshTokenRepository implements RefreshTokenRepository {
    private final JdbcTemplate jdbcTemplate;

    public JdbcRefreshTokenRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        List<RefreshToken> results = jdbcTemplate.query(
                """
                SELECT id, employee_id, token_hash, expires_at, is_revoked, created_at
                FROM refresh_tokens
                WHERE token_hash = ?
                """,
                this::mapRow,
                tokenHash);
        return results.stream().findFirst();
    }

    @Override
    public RefreshToken save(RefreshToken refreshToken) {
        if (refreshToken.getId() == null) {
            return insert(refreshToken);
        }
        return update(refreshToken);
    }

    @Override
    public void revokeById(Long id) {
        jdbcTemplate.update("UPDATE refresh_tokens SET is_revoked = TRUE WHERE id = ?", id);
    }

    @Override
    public void revokeByEmployeeId(Long employeeId) {
        jdbcTemplate.update(
                "UPDATE refresh_tokens SET is_revoked = TRUE WHERE employee_id = ? AND is_revoked = FALSE",
                employeeId);
    }

    private RefreshToken insert(RefreshToken refreshToken) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var statement = connection.prepareStatement(
                    """
                    INSERT INTO refresh_tokens (
                        employee_id,
                        token_hash,
                        expires_at,
                        is_revoked,
                        created_at
                    ) VALUES (?, ?, ?, ?, ?)
                    """,
                    Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, refreshToken.getEmployeeId());
            statement.setString(2, refreshToken.getTokenHash());
            statement.setObject(3, refreshToken.getExpiresAt());
            statement.setBoolean(4, refreshToken.isRevoked());
            statement.setObject(5, refreshToken.getCreatedAt());
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        refreshToken.setId(key == null ? null : key.longValue());
        return refreshToken;
    }

    private RefreshToken update(RefreshToken refreshToken) {
        jdbcTemplate.update(
                """
                UPDATE refresh_tokens
                SET is_revoked = ?
                WHERE id = ?
                """,
                refreshToken.isRevoked(),
                refreshToken.getId());
        return refreshToken;
    }

    private RefreshToken mapRow(ResultSet resultSet, int rowNumber) throws SQLException {
        return new RefreshToken(
                resultSet.getLong("id"),
                resultSet.getLong("employee_id"),
                resultSet.getString("token_hash"),
                resultSet.getTimestamp("expires_at").toInstant(),
                resultSet.getBoolean("is_revoked"),
                resultSet.getTimestamp("created_at").toInstant());
    }
}
