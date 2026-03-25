package com.lovius.bento.controller;

import com.lovius.bento.dto.ApiSuccessResponse;
import com.lovius.bento.dto.ImportDepartmentResult;
import com.lovius.bento.dto.ImportEmployeeResult;
import com.lovius.bento.dto.ImportMenuResult;
import com.lovius.bento.dto.ImportSupplierResult;
import com.lovius.bento.exception.ApiException;
import com.lovius.bento.security.AuthenticatedUser;
import com.lovius.bento.service.CsvImportService;
import com.lovius.bento.service.TokenService;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.RequestParam;

@Validated
@RestController
@RequestMapping("/api/admin/import")
public class A012Controller {
    private final CsvImportService csvImportService;
    private final TokenService tokenService;

    public A012Controller(CsvImportService csvImportService, TokenService tokenService) {
        this.csvImportService = csvImportService;
        this.tokenService = tokenService;
    }

    @GetMapping("/template/{type}")
    public ResponseEntity<String> downloadTemplate(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable("type") String type) {
        requireAdmin(authorizationHeader);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + type + "-template.csv\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csvImportService.getTemplate(type));
    }

    @PostMapping("/employees")
    public ApiSuccessResponse<List<ImportEmployeeResult>> importEmployees(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestParam("file") MultipartFile file) {
        requireAdmin(authorizationHeader);
        return ApiSuccessResponse.success(csvImportService.importEmployees(file));
    }

    @PostMapping("/departments")
    public ApiSuccessResponse<List<ImportDepartmentResult>> importDepartments(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestParam("file") MultipartFile file) {
        requireAdmin(authorizationHeader);
        return ApiSuccessResponse.success(csvImportService.importDepartments(file));
    }

    @PostMapping("/suppliers")
    public ApiSuccessResponse<List<ImportSupplierResult>> importSuppliers(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestParam("file") MultipartFile file) {
        requireAdmin(authorizationHeader);
        return ApiSuccessResponse.success(csvImportService.importSuppliers(file));
    }

    @PostMapping("/menus")
    public ApiSuccessResponse<List<ImportMenuResult>> importMenus(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestParam("file") MultipartFile file) {
        AuthenticatedUser authenticatedUser = requireAdmin(authorizationHeader);
        return ApiSuccessResponse.success(csvImportService.importMenus(authenticatedUser.employeeId(), file));
    }

    private AuthenticatedUser requireAdmin(String authorizationHeader) {
        AuthenticatedUser authenticatedUser = tokenService.parseToken(authorizationHeader);
        if (!"admin".equals(authenticatedUser.role())) {
            throw new ApiException(org.springframework.http.HttpStatus.FORBIDDEN, "權限不足");
        }
        return authenticatedUser;
    }
}
