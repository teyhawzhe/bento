package com.lovius.bento.controller;

import com.lovius.bento.dto.MonthlyBillingLogResponse;
import com.lovius.bento.dto.MonthlyBillingTriggerResponse;
import com.lovius.bento.exception.ApiException;
import com.lovius.bento.security.AuthenticatedUser;
import com.lovius.bento.service.MonthlyBillingService;
import com.lovius.bento.service.TokenService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/reports/monthly")
public class A005Controller {
    private final MonthlyBillingService monthlyBillingService;
    private final TokenService tokenService;

    public A005Controller(MonthlyBillingService monthlyBillingService, TokenService tokenService) {
        this.monthlyBillingService = monthlyBillingService;
        this.tokenService = tokenService;
    }

    @PostMapping
    public MonthlyBillingTriggerResponse triggerReport(
            @RequestHeader("Authorization") String authorizationHeader) {
        AuthenticatedUser user = requireAdmin(authorizationHeader);
        return monthlyBillingService.runMonthlyBilling(user.employeeId());
    }

    @GetMapping
    public List<MonthlyBillingLogResponse> getLogs(
            @RequestHeader("Authorization") String authorizationHeader) {
        requireAdmin(authorizationHeader);
        return monthlyBillingService.getLogs();
    }

    private AuthenticatedUser requireAdmin(String authorizationHeader) {
        AuthenticatedUser authenticatedUser = tokenService.parseToken(authorizationHeader);
        if (!"admin".equals(authenticatedUser.role())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "權限不足");
        }
        return authenticatedUser;
    }
}
