package com.lovius.bento.controller;

import com.lovius.bento.dto.ApiMessageResponse;
import com.lovius.bento.dto.CreateDepartmentRequest;
import com.lovius.bento.dto.DepartmentSummaryResponse;
import com.lovius.bento.dto.UpdateDepartmentRequest;
import com.lovius.bento.exception.ApiException;
import com.lovius.bento.security.AuthenticatedUser;
import com.lovius.bento.service.DepartmentService;
import com.lovius.bento.service.TokenService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/admin/departments")
public class A010Controller {
    private final DepartmentService departmentService;
    private final TokenService tokenService;

    public A010Controller(
            DepartmentService departmentService,
            TokenService tokenService) {
        this.departmentService = departmentService;
        this.tokenService = tokenService;
    }

    @GetMapping
    public List<DepartmentSummaryResponse> getDepartments(
            @RequestHeader("Authorization") String authorizationHeader) {
        requireAdmin(authorizationHeader);
        return departmentService.getAllDepartments();
    }

    @PostMapping
    public ResponseEntity<DepartmentSummaryResponse> createDepartment(
            @RequestHeader("Authorization") String authorizationHeader,
            @Valid @RequestBody CreateDepartmentRequest request) {
        requireAdmin(authorizationHeader);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(departmentService.createDepartment(request));
    }

    @PatchMapping("/{id}")
    public DepartmentSummaryResponse updateDepartment(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable("id") Long departmentId,
            @Valid @RequestBody UpdateDepartmentRequest request) {
        requireAdmin(authorizationHeader);
        return departmentService.updateDepartment(departmentId, request);
    }

    @DeleteMapping("/{id}")
    public ApiMessageResponse deleteDepartment(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable("id") Long departmentId) {
        requireAdmin(authorizationHeader);
        departmentService.deactivateDepartment(departmentId);
        return new ApiMessageResponse("部門已停用");
    }

    private AuthenticatedUser requireAdmin(String authorizationHeader) {
        AuthenticatedUser authenticatedUser = tokenService.parseToken(authorizationHeader);
        if (!"admin".equals(authenticatedUser.role())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "權限不足");
        }
        return authenticatedUser;
    }
}
