package com.lovius.bento.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lovius.bento.dto.MonthlyBillingLogResponse;
import com.lovius.bento.dto.MonthlyBillingTriggerResponse;
import com.lovius.bento.exception.GlobalExceptionHandler;
import com.lovius.bento.security.AuthenticatedUser;
import com.lovius.bento.service.MonthlyBillingService;
import com.lovius.bento.service.TokenService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(A005Controller.class)
@Import(GlobalExceptionHandler.class)
class A005ControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MonthlyBillingService monthlyBillingService;

    @MockBean
    private TokenService tokenService;

    @Test
    void triggerMonthlyBillingRequiresAdmin() throws Exception {
        when(tokenService.parseToken("Bearer employee-token"))
                .thenReturn(new AuthenticatedUser(2L, "alice", "employee"));

        mockMvc.perform(post("/api/admin/reports/monthly")
                        .header("Authorization", "Bearer employee-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("權限不足"));
    }

    @Test
    void adminCanTriggerMonthlyBilling() throws Exception {
        when(tokenService.parseToken("Bearer admin-token"))
                .thenReturn(new AuthenticatedUser(1L, "admin", "admin"));
        when(monthlyBillingService.runMonthlyBilling(1L)).thenReturn(new MonthlyBillingTriggerResponse(
                "月結帳單報表已完成處理",
                LocalDate.of(2026, 2, 15),
                LocalDate.of(2026, 3, 14),
                2,
                4,
                1));

        mockMvc.perform(post("/api/admin/reports/monthly")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.supplierCount").value(2))
                .andExpect(jsonPath("$.failedCount").value(1));
    }

    @Test
    void adminCanQueryMonthlyBillingLogs() throws Exception {
        when(tokenService.parseToken("Bearer admin-token"))
                .thenReturn(new AuthenticatedUser(1L, "admin", "admin"));
        when(monthlyBillingService.getLogs()).thenReturn(List.of(
                new MonthlyBillingLogResponse(
                        10L,
                        LocalDate.of(2026, 2, 15),
                        LocalDate.of(2026, 3, 14),
                        1L,
                        "月結便當",
                        "supplier@company.local",
                        "sent",
                        null,
                        1L,
                        Instant.parse("2026-03-15T01:00:00Z"),
                        Instant.parse("2026-03-15T01:00:00Z"))));

        mockMvc.perform(get("/api/admin/reports/monthly")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].supplierName").value("月結便當"))
                .andExpect(jsonPath("$[0].emailTo").value("supplier@company.local"));
    }
}
