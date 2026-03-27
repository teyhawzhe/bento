package com.lovius.bento.controller;

import com.lovius.bento.dto.ApiSuccessResponse;
import com.lovius.bento.dto.EmployeeOrderReportResponse;
import com.lovius.bento.exception.ApiException;
import com.lovius.bento.security.AuthenticatedUser;
import com.lovius.bento.service.EmployeeOrderReportService;
import com.lovius.bento.service.TokenService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/reports/orders")
public class A013Controller {
    private final EmployeeOrderReportService employeeOrderReportService;
    private final TokenService tokenService;

    public A013Controller(EmployeeOrderReportService employeeOrderReportService, TokenService tokenService) {
        this.employeeOrderReportService = employeeOrderReportService;
        this.tokenService = tokenService;
    }

    @GetMapping
    public ApiSuccessResponse<List<EmployeeOrderReportResponse>> getOrderReports(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestParam(name = "date_from", required = false) LocalDate dateFrom,
            @RequestParam(name = "date_to", required = false) LocalDate dateTo,
            @RequestParam(name = "sort_by", required = false) String sortBy) {
        requireAdmin(authorizationHeader);
        return ApiSuccessResponse.success(employeeOrderReportService.getReport(dateFrom, dateTo, sortBy));
    }

    @GetMapping("/pdf")
    public ResponseEntity<byte[]> downloadOrderReportPdf(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestParam(name = "date_from", required = false) LocalDate dateFrom,
            @RequestParam(name = "date_to", required = false) LocalDate dateTo,
            @RequestParam(name = "sort_by", required = false) String sortBy) {
        requireAdmin(authorizationHeader);
        byte[] pdfContent = employeeOrderReportService.downloadPdf(dateFrom, dateTo, sortBy);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=employee-order-report.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfContent);
    }

    private AuthenticatedUser requireAdmin(String authorizationHeader) {
        AuthenticatedUser authenticatedUser = tokenService.parseToken(authorizationHeader);
        if (!"admin".equals(authenticatedUser.role())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "權限不足");
        }
        return authenticatedUser;
    }
}
