package com.lovius.bento.controller;

import com.lovius.bento.dto.ApiSuccessResponse;
import com.lovius.bento.dto.MenuCheckNotificationResponse;
import com.lovius.bento.exception.ApiException;
import com.lovius.bento.security.AuthenticatedUser;
import com.lovius.bento.service.MenuCheckNotificationService;
import com.lovius.bento.service.TokenService;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/admin/notifications/menu-check")
public class A015Controller {
    private final MenuCheckNotificationService menuCheckNotificationService;
    private final TokenService tokenService;

    public A015Controller(
            MenuCheckNotificationService menuCheckNotificationService,
            TokenService tokenService) {
        this.menuCheckNotificationService = menuCheckNotificationService;
        this.tokenService = tokenService;
    }

    @GetMapping
    public ApiSuccessResponse<MenuCheckNotificationResponse> getMenuCheck(
            @RequestHeader("Authorization") String authorizationHeader) {
        requireAdmin(authorizationHeader);
        return ApiSuccessResponse.success(new MenuCheckNotificationResponse(
                menuCheckNotificationService.getMissingDatesForAdmin()));
    }

    @PostMapping("/dismiss")
    public ApiSuccessResponse<Void> dismissMenuCheck(
            @RequestHeader("Authorization") String authorizationHeader) {
        requireAdmin(authorizationHeader);
        menuCheckNotificationService.dismissToday();
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
