package com.lovius.bento.controller;

import com.lovius.bento.dto.ApiMessageResponse;
import com.lovius.bento.dto.ApiSuccessResponse;
import com.lovius.bento.dto.ChangePasswordRequest;
import com.lovius.bento.dto.CreateEmployeeRequest;
import com.lovius.bento.dto.EmployeeStatusRequest;
import com.lovius.bento.dto.EmployeeSummaryResponse;
import com.lovius.bento.dto.ForgotPasswordRequest;
import com.lovius.bento.dto.ImportEmployeesResponse;
import com.lovius.bento.dto.LoginRequest;
import com.lovius.bento.dto.LoginResponse;
import com.lovius.bento.dto.RefreshTokenRequest;
import com.lovius.bento.dto.ResetEmployeePasswordRequest;
import com.lovius.bento.dto.UpdateEmployeeRequest;
import com.lovius.bento.exception.ApiException;
import com.lovius.bento.security.AuthenticatedUser;
import com.lovius.bento.service.AuthService;
import com.lovius.bento.service.EmployeeService;
import com.lovius.bento.service.TokenService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Validated
@RestController
@RequestMapping("/api")
public class A001Controller {
    private final AuthService authService;
    private final EmployeeService employeeService;
    private final TokenService tokenService;

    public A001Controller(
            AuthService authService,
            EmployeeService employeeService,
            TokenService tokenService) {
        this.authService = authService;
        this.employeeService = employeeService;
        this.tokenService = tokenService;
    }

    @PostMapping("/auth/login")
    public ApiSuccessResponse<LoginResponse> employeeLogin(@Valid @RequestBody LoginRequest request) {
        return ApiSuccessResponse.success(authService.login(request, false));
    }

    @PostMapping("/auth/refresh")
    public ApiSuccessResponse<LoginResponse> employeeRefresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiSuccessResponse.success(authService.refresh(request, false));
    }

    @PostMapping("/admin/auth/login")
    public ApiSuccessResponse<LoginResponse> adminLogin(@Valid @RequestBody LoginRequest request) {
        return ApiSuccessResponse.success(authService.login(request, true));
    }

    @PostMapping("/admin/auth/refresh")
    public ApiSuccessResponse<LoginResponse> adminRefresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiSuccessResponse.success(authService.refresh(request, true));
    }

    @PostMapping("/auth/logout")
    public ApiSuccessResponse<Void> logout(
            @RequestHeader("Authorization") String authorizationHeader) {
        AuthenticatedUser authenticatedUser = requireRole(authorizationHeader, "employee");
        authService.logout(authenticatedUser);
        return ApiSuccessResponse.empty();
    }

    @PostMapping("/auth/forgot-password")
    public ApiSuccessResponse<Void> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ApiSuccessResponse.empty();
    }

    @PatchMapping("/auth/change-password")
    public ApiSuccessResponse<Void> changePassword(
            @RequestHeader("Authorization") String authorizationHeader,
            @Valid @RequestBody ChangePasswordRequest request) {
        AuthenticatedUser authenticatedUser = requireRole(authorizationHeader, "employee");
        authService.changePassword(authenticatedUser, request);
        return ApiSuccessResponse.empty();
    }

    @GetMapping("/admin/employees")
    public ApiSuccessResponse<List<EmployeeSummaryResponse>> getEmployees(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestParam(value = "department_id", required = false) Long departmentId) {
        requireRole(authorizationHeader, "admin");
        return ApiSuccessResponse.success(employeeService.getEmployees(departmentId));
    }

    @PostMapping("/admin/employees")
    public ResponseEntity<ApiSuccessResponse<Void>> createEmployee(
            @RequestHeader("Authorization") String authorizationHeader,
            @Valid @RequestBody CreateEmployeeRequest request) {
        requireRole(authorizationHeader, "admin");
        employeeService.createEmployee(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiSuccessResponse.empty());
    }

    @PostMapping("/admin/employees/import")
    public ApiSuccessResponse<ImportEmployeesResponse> importEmployees(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestParam("file") MultipartFile file) {
        requireRole(authorizationHeader, "admin");
        return ApiSuccessResponse.success(employeeService.importEmployees(file));
    }

    @PatchMapping("/admin/employees/{id}")
    public ApiSuccessResponse<EmployeeSummaryResponse> updateEmployee(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable("id") Long employeeId,
            @Valid @RequestBody UpdateEmployeeRequest request) {
        requireRole(authorizationHeader, "admin");
        return ApiSuccessResponse.success(employeeService.updateEmployee(employeeId, request));
    }

    @PatchMapping("/admin/employees/{id}/status")
    public ApiSuccessResponse<Void> updateEmployeeStatus(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable("id") Long employeeId,
            @Valid @RequestBody EmployeeStatusRequest request) {
        requireRole(authorizationHeader, "admin");
        employeeService.updateStatus(employeeId, request);
        return ApiSuccessResponse.empty();
    }

    @PatchMapping("/admin/employees/{id}/reset-password")
    public ApiSuccessResponse<Void> resetPassword(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable("id") Long employeeId,
            @Valid @RequestBody ResetEmployeePasswordRequest request) {
        requireRole(authorizationHeader, "admin");
        employeeService.resetPassword(employeeId, request);
        return ApiSuccessResponse.empty();
    }

    private AuthenticatedUser requireRole(String authorizationHeader, String expectedRole) {
        AuthenticatedUser authenticatedUser = tokenService.parseToken(authorizationHeader);
        if (!expectedRole.equals(authenticatedUser.role())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "沒有操作權限");
        }
        return authenticatedUser;
    }
}
