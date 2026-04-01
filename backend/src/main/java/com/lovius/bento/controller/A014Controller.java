package com.lovius.bento.controller;

import com.lovius.bento.dto.ApiSuccessResponse;
import com.lovius.bento.dto.WorkCalendarDayDto;
import com.lovius.bento.dto.WorkCalendarGenerateRequest;
import com.lovius.bento.exception.ApiException;
import com.lovius.bento.security.AuthenticatedUser;
import com.lovius.bento.service.TokenService;
import com.lovius.bento.service.WorkCalendarService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Validated
@RestController
@RequestMapping("/api/admin/calendar")
public class A014Controller {
    private final WorkCalendarService workCalendarService;
    private final TokenService tokenService;

    public A014Controller(WorkCalendarService workCalendarService, TokenService tokenService) {
        this.workCalendarService = workCalendarService;
        this.tokenService = tokenService;
    }

    @GetMapping
    public ApiSuccessResponse<List<WorkCalendarDayDto>> getCalendar(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestParam("year") @Min(2000) @Max(2100) int year,
            @RequestParam("month") @Min(1) @Max(12) int month) {
        requireAdmin(authorizationHeader);
        return ApiSuccessResponse.success(workCalendarService.getCalendar(year, month));
    }

    @PutMapping
    public ApiSuccessResponse<Void> updateCalendar(
            @RequestHeader("Authorization") String authorizationHeader,
            @Valid @RequestBody List<@Valid WorkCalendarDayDto> days) {
        requireAdmin(authorizationHeader);
        workCalendarService.updateCalendar(days);
        return ApiSuccessResponse.empty();
    }

    @PostMapping("/generate")
    public ApiSuccessResponse<Void> generateCalendar(
            @RequestHeader("Authorization") String authorizationHeader,
            @Valid @RequestBody WorkCalendarGenerateRequest request) {
        requireAdmin(authorizationHeader);
        workCalendarService.generateCalendar(request.year());
        return ApiSuccessResponse.empty();
    }

    @PostMapping("/import")
    public ApiSuccessResponse<List<WorkCalendarDayDto>> importCalendar(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestParam("file") MultipartFile file,
            @RequestParam("confirm") boolean confirm) {
        requireAdmin(authorizationHeader);
        return ApiSuccessResponse.success(workCalendarService.importCalendar(file, confirm));
    }

    private AuthenticatedUser requireAdmin(String authorizationHeader) {
        AuthenticatedUser authenticatedUser = tokenService.parseToken(authorizationHeader);
        if (!"admin".equals(authenticatedUser.role())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "權限不足");
        }
        return authenticatedUser;
    }
}
