package com.lovius.bento.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lovius.bento.dto.CreateReportEmailRequest;
import com.lovius.bento.dto.ReportEmailResponse;
import com.lovius.bento.exception.ApiException;
import com.lovius.bento.exception.GlobalExceptionHandler;
import com.lovius.bento.security.AuthenticatedUser;
import com.lovius.bento.service.ReportEmailSettingsService;
import com.lovius.bento.service.TokenService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(A008Controller.class)
@Import(GlobalExceptionHandler.class)
class A008ControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReportEmailSettingsService reportEmailSettingsService;

    @MockBean
    private TokenService tokenService;

    @Test
    void getReportEmailsReturnsListForAdmin() throws Exception {
        when(tokenService.parseToken("Bearer admin-token"))
                .thenReturn(new AuthenticatedUser(1L, "admin", "admin"));
        when(reportEmailSettingsService.getAll()).thenReturn(List.of(
                new ReportEmailResponse(1L, "finance@company.local", 1L, Instant.parse("2026-03-21T10:00:00Z"))));

        mockMvc.perform(get("/api/settings/report-emails")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].email").value("finance@company.local"));
    }

    @Test
    void createReportEmailRejectsInvalidFormat() throws Exception {
        when(tokenService.parseToken("Bearer admin-token"))
                .thenReturn(new AuthenticatedUser(1L, "admin", "admin"));

        mockMvc.perform(post("/api/settings/report-emails")
                        .header("Authorization", "Bearer admin-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "not-an-email"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.message").value("報表收件 Email 格式錯誤"));
    }

    @Test
    void deleteReportEmailRejectsNonAdmin() throws Exception {
        when(tokenService.parseToken("Bearer employee-token"))
                .thenReturn(new AuthenticatedUser(2L, "alice", "employee"));

        mockMvc.perform(delete("/api/settings/report-emails/5")
                        .header("Authorization", "Bearer employee-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.data.message").value("權限不足"));
    }

    @Test
    void createReportEmailDelegatesToService() throws Exception {
        when(tokenService.parseToken("Bearer admin-token"))
                .thenReturn(new AuthenticatedUser(1L, "admin", "admin"));
        when(reportEmailSettingsService.create(1L, new CreateReportEmailRequest("finance@company.local")))
                .thenReturn(new ReportEmailResponse(9L, "finance@company.local", 1L, Instant.parse("2026-03-21T10:00:00Z")));

        mockMvc.perform(post("/api/settings/report-emails")
                        .header("Authorization", "Bearer admin-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "finance@company.local"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(9))
                .andExpect(jsonPath("$.data.email").value("finance@company.local"));

        verify(reportEmailSettingsService).create(1L, new CreateReportEmailRequest("finance@company.local"));
    }

    @Test
    void deleteReportEmailReturnsNotFoundWhenMissing() throws Exception {
        when(tokenService.parseToken("Bearer admin-token"))
                .thenReturn(new AuthenticatedUser(1L, "admin", "admin"));
        org.mockito.Mockito.doThrow(new ApiException(HttpStatus.NOT_FOUND, "查無報表收件信箱"))
                .when(reportEmailSettingsService)
                .delete(99L);

        mockMvc.perform(delete("/api/settings/report-emails/99")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.data.message").value("查無報表收件信箱"));
    }
}
