package com.lovius.bento.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lovius.bento.exception.GlobalExceptionHandler;
import com.lovius.bento.security.AuthenticatedUser;
import com.lovius.bento.service.MenuCheckNotificationService;
import com.lovius.bento.service.TokenService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(A015Controller.class)
@Import(GlobalExceptionHandler.class)
class A015ControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MenuCheckNotificationService menuCheckNotificationService;

    @MockBean
    private TokenService tokenService;

    @Test
    void getMenuCheckRequiresAdmin() throws Exception {
        when(tokenService.parseToken("Bearer employee-token"))
                .thenReturn(new AuthenticatedUser(2L, "alice", "employee"));

        mockMvc.perform(get("/api/admin/notifications/menu-check")
                        .header("Authorization", "Bearer employee-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.data.message").value("權限不足"));
    }

    @Test
    void adminCanGetMissingDates() throws Exception {
        when(tokenService.parseToken("Bearer admin-token"))
                .thenReturn(new AuthenticatedUser(1L, "admin", "admin"));
        when(menuCheckNotificationService.getMissingDatesForAdmin())
                .thenReturn(List.of(LocalDate.of(2026, 4, 3), LocalDate.of(2026, 4, 7)));

        mockMvc.perform(get("/api/admin/notifications/menu-check")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.missing_dates[0]").value("2026-04-03"))
                .andExpect(jsonPath("$.data.missing_dates[1]").value("2026-04-07"));
    }

    @Test
    void adminCanDismissReminder() throws Exception {
        when(tokenService.parseToken("Bearer admin-token"))
                .thenReturn(new AuthenticatedUser(1L, "admin", "admin"));

        mockMvc.perform(post("/api/admin/notifications/menu-check/dismiss")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        verify(menuCheckNotificationService).dismissToday();
    }
}
