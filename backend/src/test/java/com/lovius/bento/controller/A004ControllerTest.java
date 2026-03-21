package com.lovius.bento.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lovius.bento.dto.CreateErrorEmailRequest;
import com.lovius.bento.dto.ErrorEmailResponse;
import com.lovius.bento.exception.ApiException;
import com.lovius.bento.exception.GlobalExceptionHandler;
import com.lovius.bento.security.AuthenticatedUser;
import com.lovius.bento.service.ErrorEmailSettingsService;
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

@WebMvcTest(A004Controller.class)
@Import(GlobalExceptionHandler.class)
class A004ControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ErrorEmailSettingsService errorEmailSettingsService;

    @MockBean
    private TokenService tokenService;

    @Test
    void getErrorEmailsReturnsListForAdmin() throws Exception {
        when(tokenService.parseToken("Bearer admin-token"))
                .thenReturn(new AuthenticatedUser(1L, "admin", "admin"));
        when(errorEmailSettingsService.getAll()).thenReturn(List.of(
                new ErrorEmailResponse(1L, "ops-alerts@company.local", 1L, Instant.parse("2026-03-21T10:00:00Z"))));

        mockMvc.perform(get("/api/settings/error-emails")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("ops-alerts@company.local"));
    }

    @Test
    void createErrorEmailRejectsInvalidEmailFormat() throws Exception {
        when(tokenService.parseToken("Bearer admin-token"))
                .thenReturn(new AuthenticatedUser(1L, "admin", "admin"));

        mockMvc.perform(post("/api/settings/error-emails")
                        .header("Authorization", "Bearer admin-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "not-an-email"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("錯誤通知 Email 格式錯誤"));
    }

    @Test
    void deleteErrorEmailRejectsNonAdmin() throws Exception {
        when(tokenService.parseToken("Bearer employee-token"))
                .thenReturn(new AuthenticatedUser(2L, "alice", "employee"));

        mockMvc.perform(delete("/api/settings/error-emails/5")
                        .header("Authorization", "Bearer employee-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("權限不足"));
    }

    @Test
    void createErrorEmailDelegatesToService() throws Exception {
        when(tokenService.parseToken("Bearer admin-token"))
                .thenReturn(new AuthenticatedUser(1L, "admin", "admin"));
        when(errorEmailSettingsService.create(1L, new CreateErrorEmailRequest("ops@company.local")))
                .thenReturn(new ErrorEmailResponse(9L, "ops@company.local", 1L, Instant.parse("2026-03-21T10:00:00Z")));

        mockMvc.perform(post("/api/settings/error-emails")
                        .header("Authorization", "Bearer admin-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "ops@company.local"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(9))
                .andExpect(jsonPath("$.email").value("ops@company.local"));

        verify(errorEmailSettingsService).create(1L, new CreateErrorEmailRequest("ops@company.local"));
    }

    @Test
    void deleteErrorEmailReturnsNotFoundWhenMissing() throws Exception {
        when(tokenService.parseToken("Bearer admin-token"))
                .thenReturn(new AuthenticatedUser(1L, "admin", "admin"));
        org.mockito.Mockito.doThrow(new ApiException(HttpStatus.NOT_FOUND, "查無錯誤通知信箱"))
                .when(errorEmailSettingsService)
                .delete(99L);

        mockMvc.perform(delete("/api/settings/error-emails/99")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("查無錯誤通知信箱"));
    }
}
