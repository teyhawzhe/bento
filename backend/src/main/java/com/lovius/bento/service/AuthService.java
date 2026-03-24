package com.lovius.bento.service;

import com.lovius.bento.dao.EmployeeRepository;
import com.lovius.bento.dto.ApiMessageResponse;
import com.lovius.bento.dto.ChangePasswordRequest;
import com.lovius.bento.dto.ForgotPasswordRequest;
import com.lovius.bento.dto.LoginRequest;
import com.lovius.bento.dto.LoginResponse;
import com.lovius.bento.dto.RefreshTokenRequest;
import com.lovius.bento.exception.ApiException;
import com.lovius.bento.model.Employee;
import com.lovius.bento.security.AuthenticatedUser;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final EmployeeRepository employeeRepository;
    private final PasswordPolicyService passwordPolicyService;
    private final EmailService emailService;
    private final RefreshTokenService refreshTokenService;

    public AuthService(
            EmployeeRepository employeeRepository,
            PasswordPolicyService passwordPolicyService,
            EmailService emailService,
            RefreshTokenService refreshTokenService) {
        this.employeeRepository = employeeRepository;
        this.passwordPolicyService = passwordPolicyService;
        this.emailService = emailService;
        this.refreshTokenService = refreshTokenService;
    }

    public LoginResponse login(LoginRequest request, boolean adminLogin) {
        Employee employee = employeeRepository.findByUsername(request.username())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "帳號不存在"));

        if (adminLogin && !employee.isAdmin()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "此入口僅限管理員登入");
        }
        if (!adminLogin && employee.isAdmin()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "請使用管理員登入入口");
        }
        if (!passwordPolicyService.matches(request.password(), employee.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "密碼錯誤");
        }
        if (!employee.isActive()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "帳號已停用");
        }

        String role = adminLogin ? "admin" : "employee";
        return refreshTokenService.issueTokens(employee, role);
    }

    public LoginResponse refresh(RefreshTokenRequest request, boolean adminRefresh) {
        return refreshTokenService.refresh(request.refreshToken(), adminRefresh);
    }

    public ApiMessageResponse logout(AuthenticatedUser authenticatedUser) {
        refreshTokenService.revokeAllByEmployeeId(authenticatedUser.employeeId());
        return new ApiMessageResponse("已成功登出");
    }

    public ApiMessageResponse forgotPassword(ForgotPasswordRequest request) {
        Employee employee = employeeRepository.findByEmail(request.email())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "查無此 Email"));
        String temporaryPassword = passwordPolicyService.generateTemporaryPassword();
        employee.setPasswordHash(passwordPolicyService.hash(temporaryPassword));
        employee.touchUpdatedAt(Instant.now());
        employeeRepository.save(employee);
        refreshTokenService.revokeAllByEmployeeId(employee.getId());
        emailService.sendPasswordEmail(employee.getEmail(), "臨時密碼通知", temporaryPassword);
        return new ApiMessageResponse("臨時密碼已寄送至信箱");
    }

    public ApiMessageResponse changePassword(
            AuthenticatedUser authenticatedUser,
            ChangePasswordRequest request) {
        Employee employee = employeeRepository.findById(authenticatedUser.employeeId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "查無使用者"));
        passwordPolicyService.validatePassword(request.newPassword());
        if (!passwordPolicyService.matches(request.oldPassword(), employee.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "舊密碼錯誤");
        }
        employee.setPasswordHash(passwordPolicyService.hash(request.newPassword()));
        employee.touchUpdatedAt(Instant.now());
        employeeRepository.save(employee);
        refreshTokenService.revokeAllByEmployeeId(employee.getId());
        return new ApiMessageResponse("密碼修改成功，請重新登入");
    }
}
