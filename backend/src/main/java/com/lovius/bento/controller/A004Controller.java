package com.lovius.bento.controller;

import com.lovius.bento.dto.ApiSuccessResponse;
import com.lovius.bento.dto.CreateErrorEmailRequest;
import com.lovius.bento.dto.ErrorEmailResponse;
import com.lovius.bento.exception.ApiException;
import com.lovius.bento.security.AuthenticatedUser;
import com.lovius.bento.service.ErrorEmailSettingsService;
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
@RequestMapping("/api/settings/error-emails")
public class A004Controller {
    private final ErrorEmailSettingsService errorEmailSettingsService;
    private final TokenService tokenService;

    public A004Controller(
            ErrorEmailSettingsService errorEmailSettingsService,
            TokenService tokenService) {
        this.errorEmailSettingsService = errorEmailSettingsService;
        this.tokenService = tokenService;
    }

    @GetMapping
    public ApiSuccessResponse<List<ErrorEmailResponse>> getErrorEmails(
            @RequestHeader("Authorization") String authorizationHeader) {
        requireAdmin(authorizationHeader);
        return ApiSuccessResponse.success(errorEmailSettingsService.getAll());
    }

    @PostMapping
    public ResponseEntity<ApiSuccessResponse<ErrorEmailResponse>> createErrorEmail(
            @RequestHeader("Authorization") String authorizationHeader,
            @Valid @RequestBody CreateErrorEmailRequest request) {
        AuthenticatedUser user = requireAdmin(authorizationHeader);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiSuccessResponse.success(errorEmailSettingsService.create(user.employeeId(), request)));
    }

    @DeleteMapping("/{id}")
    public ApiSuccessResponse<Void> deleteErrorEmail(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable("id") Long id) {
        requireAdmin(authorizationHeader);
        errorEmailSettingsService.delete(id);
        return ApiSuccessResponse.empty();
    }

    private AuthenticatedUser requireAdmin(String authorizationHeader) {
        AuthenticatedUser authenticatedUser = tokenService.parseToken(authorizationHeader);
        if (!"admin".equals(authenticatedUser.role())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "權限不足");
        }
        return authenticatedUser;
    }
}
