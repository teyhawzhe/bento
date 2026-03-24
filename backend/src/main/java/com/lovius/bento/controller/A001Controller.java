package com.lovius.bento.controller;

import com.lovius.bento.dto.ApiMessageResponse;
import com.lovius.bento.dto.ChangePasswordRequest;
import com.lovius.bento.dto.CreateEmployeeRequest;
import com.lovius.bento.dto.EmployeeCreatedResponse;
import com.lovius.bento.dto.EmployeeStatusRequest;
import com.lovius.bento.dto.EmployeeSummaryResponse;
import com.lovius.bento.dto.ForgotPasswordRequest;
import com.lovius.bento.dto.ImportEmployeesResponse;
import com.lovius.bento.dto.LoginRequest;
import com.lovius.bento.dto.LoginResponse;
import com.lovius.bento.dto.ResetEmployeePasswordRequest;
import com.lovius.bento.dto.UpdateEmployeeRequest;
import com.lovius.bento.dto.UpdateEmployeeResponse;
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
    public LoginResponse employeeLogin(@Valid @RequestBody LoginRequest request) {
        return authService.login(request, false);
    }

    @PostMapping("/admin/auth/login")
    public LoginResponse adminLogin(@Valid @RequestBody LoginRequest request) {
        return authService.login(request, true);
    }

    @PostMapping("/auth/logout")
    public ApiMessageResponse logout() {
        return authService.logout();
    }

    @PostMapping("/auth/forgot-password")
    public ApiMessageResponse forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        return authService.forgotPassword(request);
    }

    @PatchMapping("/auth/change-password")
    public ApiMessageResponse changePassword(
            @RequestHeader("Authorization") String authorizationHeader,
            @Valid @RequestBody ChangePasswordRequest request) {
        AuthenticatedUser authenticatedUser = requireRole(authorizationHeader, "employee");
        return authService.changePassword(authenticatedUser, request);
    }

    @GetMapping("/admin/employees")
    public List<EmployeeSummaryResponse> getEmployees(
            @RequestHeader("Authorization") String authorizationHeader) {
        requireRole(authorizationHeader, "admin");
        return employeeService.getAllEmployees();
    }

    @PostMapping("/admin/employees")
    public ResponseEntity<EmployeeCreatedResponse> createEmployee(
            @RequestHeader("Authorization") String authorizationHeader,
            @Valid @RequestBody CreateEmployeeRequest request) {
        requireRole(authorizationHeader, "admin");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(employeeService.createEmployee(request));
    }

    @PostMapping("/admin/employees/import")
    public ImportEmployeesResponse importEmployees(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestParam("file") MultipartFile file) {
        requireRole(authorizationHeader, "admin");
        return employeeService.importEmployees(file);
    }

    @PatchMapping("/admin/employees/{id}")
    public UpdateEmployeeResponse updateEmployee(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable("id") Long employeeId,
            @Valid @RequestBody UpdateEmployeeRequest request) {
        requireRole(authorizationHeader, "admin");
        return employeeService.updateEmployee(employeeId, request);
    }

    @PatchMapping("/admin/employees/{id}/status")
    public EmployeeSummaryResponse updateEmployeeStatus(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable("id") Long employeeId,
            @Valid @RequestBody EmployeeStatusRequest request) {
        requireRole(authorizationHeader, "admin");
        return employeeService.updateStatus(employeeId, request);
    }

    @PatchMapping("/admin/employees/{id}/reset-password")
    public EmployeeSummaryResponse resetPassword(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable("id") Long employeeId,
            @Valid @RequestBody ResetEmployeePasswordRequest request) {
        requireRole(authorizationHeader, "admin");
        return employeeService.resetPassword(employeeId, request);
    }

    private AuthenticatedUser requireRole(String authorizationHeader, String expectedRole) {
        AuthenticatedUser authenticatedUser = tokenService.parseToken(authorizationHeader);
        if (!expectedRole.equals(authenticatedUser.role())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "沒有操作權限");
        }
        return authenticatedUser;
    }
}
