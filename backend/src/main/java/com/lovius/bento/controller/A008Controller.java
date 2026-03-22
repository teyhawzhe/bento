package com.lovius.bento.controller;

import com.lovius.bento.dto.ApiMessageResponse;
import com.lovius.bento.dto.CreateReportEmailRequest;
import com.lovius.bento.dto.ReportEmailResponse;
import com.lovius.bento.exception.ApiException;
import com.lovius.bento.security.AuthenticatedUser;
import com.lovius.bento.service.ReportEmailSettingsService;
import com.lovius.bento.service.TokenService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/settings/report-emails")
public class A008Controller {
    private final ReportEmailSettingsService reportEmailSettingsService;
    private final TokenService tokenService;

    public A008Controller(
            ReportEmailSettingsService reportEmailSettingsService,
            TokenService tokenService) {
        this.reportEmailSettingsService = reportEmailSettingsService;
        this.tokenService = tokenService;
    }

    @GetMapping
    public List<ReportEmailResponse> getReportEmails(
            @RequestHeader("Authorization") String authorizationHeader) {
        requireAdmin(authorizationHeader);
        return reportEmailSettingsService.getAll();
    }

    @PostMapping
    public ResponseEntity<ReportEmailResponse> createReportEmail(
            @RequestHeader("Authorization") String authorizationHeader,
            @Valid @RequestBody CreateReportEmailRequest request) {
        AuthenticatedUser user = requireAdmin(authorizationHeader);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reportEmailSettingsService.create(user.employeeId(), request));
    }

    @DeleteMapping("/{id}")
    public ApiMessageResponse deleteReportEmail(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable("id") Long id) {
        requireAdmin(authorizationHeader);
        reportEmailSettingsService.delete(id);
        return new ApiMessageResponse("報表收件信箱已刪除");
    }

    private AuthenticatedUser requireAdmin(String authorizationHeader) {
        AuthenticatedUser authenticatedUser = tokenService.parseToken(authorizationHeader);
        if (!"admin".equals(authenticatedUser.role())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "權限不足");
        }
        return authenticatedUser;
    }
}
