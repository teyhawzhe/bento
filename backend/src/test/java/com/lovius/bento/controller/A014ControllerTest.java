package com.lovius.bento.controller;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lovius.bento.dto.WorkCalendarDayDto;
import com.lovius.bento.exception.GlobalExceptionHandler;
import com.lovius.bento.security.AuthenticatedUser;
import com.lovius.bento.service.TokenService;
import com.lovius.bento.service.WorkCalendarService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(A014Controller.class)
@Import(GlobalExceptionHandler.class)
class A014ControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WorkCalendarService workCalendarService;

    @MockBean
    private TokenService tokenService;

    @Test
    void calendarRequiresAdmin() throws Exception {
        when(tokenService.parseToken("Bearer employee-token"))
                .thenReturn(new AuthenticatedUser(2L, "alice", "employee"));

        mockMvc.perform(get("/api/admin/calendar")
                        .header("Authorization", "Bearer employee-token")
                        .param("year", "2026")
                        .param("month", "4"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.data.message").value("權限不足"));
    }

    @Test
    void adminCanQueryCalendar() throws Exception {
        when(tokenService.parseToken("Bearer admin-token"))
                .thenReturn(new AuthenticatedUser(1L, "admin", "admin"));
        when(workCalendarService.getCalendar(2026, 4))
                .thenReturn(List.of(new WorkCalendarDayDto(LocalDate.of(2026, 4, 1), true)));

        mockMvc.perform(get("/api/admin/calendar")
                        .header("Authorization", "Bearer admin-token")
                        .param("year", "2026")
                        .param("month", "4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].date").value("2026-04-01"))
                .andExpect(jsonPath("$.data[0].is_workday").value(true));
    }

    @Test
    void adminCanUpdateCalendar() throws Exception {
        when(tokenService.parseToken("Bearer admin-token"))
                .thenReturn(new AuthenticatedUser(1L, "admin", "admin"));

        mockMvc.perform(put("/api/admin/calendar")
                        .header("Authorization", "Bearer admin-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                [
                                  {"date":"2026-04-01","is_workday":true},
                                  {"date":"2026-04-02","is_workday":false}
                                ]
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        verify(workCalendarService).updateCalendar(anyList());
    }

    @Test
    void adminCanGenerateCalendar() throws Exception {
        when(tokenService.parseToken("Bearer admin-token"))
                .thenReturn(new AuthenticatedUser(1L, "admin", "admin"));

        mockMvc.perform(post("/api/admin/calendar/generate")
                        .header("Authorization", "Bearer admin-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"year":2026}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        verify(workCalendarService).generateCalendar(2026);
    }

    @Test
    void adminCanPreviewImportCalendar() throws Exception {
        when(tokenService.parseToken("Bearer admin-token"))
                .thenReturn(new AuthenticatedUser(1L, "admin", "admin"));
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "calendar.csv",
                "text/csv",
                "20260401,y\n".getBytes());
        when(workCalendarService.importCalendar(org.mockito.ArgumentMatchers.any(), eq(false)))
                .thenReturn(List.of(new WorkCalendarDayDto(LocalDate.of(2026, 4, 1), true)));

        mockMvc.perform(multipart("/api/admin/calendar/import")
                        .file(file)
                        .param("confirm", "false")
                        .with(request -> {
                            request.setMethod("POST");
                            request.addHeader("Authorization", "Bearer admin-token");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].date").value("2026-04-01"))
                .andExpect(jsonPath("$.data[0].is_workday").value(true));

        verify(workCalendarService).importCalendar(org.mockito.ArgumentMatchers.any(), eq(false));
    }
}
