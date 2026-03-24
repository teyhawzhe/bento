package com.lovius.bento.service;

import com.lovius.bento.dao.EmployeeRepository;
import com.lovius.bento.dao.RefreshTokenRepository;
import com.lovius.bento.dto.LoginResponse;
import com.lovius.bento.exception.ApiException;
import com.lovius.bento.model.Employee;
import com.lovius.bento.model.RefreshToken;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;
    private final EmployeeRepository employeeRepository;
    private final TokenService tokenService;
    private final long refreshExpirationDays;

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            EmployeeRepository employeeRepository,
            TokenService tokenService,
            @Value("${app.jwt.refresh-expiration-days}") long refreshExpirationDays) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.employeeRepository = employeeRepository;
        this.tokenService = tokenService;
        this.refreshExpirationDays = refreshExpirationDays;
    }

    public LoginResponse issueTokens(Employee employee, String role) {
        String accessToken = tokenService.generateToken(employee.getId(), employee.getUsername(), role);
        String refreshTokenValue = UUID.randomUUID().toString() + UUID.randomUUID();
        Instant now = Instant.now();
        RefreshToken refreshToken = new RefreshToken(
                null,
                employee.getId(),
                hash(refreshTokenValue),
                now.plus(refreshExpirationDays, ChronoUnit.DAYS),
                false,
                now);
        refreshTokenRepository.save(refreshToken);
        return new LoginResponse(
                accessToken,
                refreshTokenValue,
                role,
                employee.getId(),
                employee.getUsername(),
                employee.getName());
    }

    public LoginResponse refresh(String rawRefreshToken, boolean adminRefresh) {
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(hash(rawRefreshToken))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Refresh Token 驗證失敗"));
        if (refreshToken.isRevoked() || refreshToken.getExpiresAt().isBefore(Instant.now())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Refresh Token 驗證失敗");
        }

        Employee employee = employeeRepository.findById(refreshToken.getEmployeeId())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Refresh Token 驗證失敗"));
        if (!employee.isActive()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "帳號已停用");
        }

        String role = adminRefresh ? "admin" : "employee";
        if (adminRefresh && !employee.isAdmin()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "此入口僅限管理員使用");
        }
        if (!adminRefresh && employee.isAdmin()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "請使用管理員 refresh 入口");
        }

        refreshTokenRepository.revokeById(refreshToken.getId());
        return issueTokens(employee, role);
    }

    public void revokeAllByEmployeeId(Long employeeId) {
        refreshTokenRepository.revokeByEmployeeId(employeeId);
    }

    private String hash(String rawRefreshToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawRefreshToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm not available", exception);
        }
    }
}
