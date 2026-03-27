package com.lovius.bento.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lovius.bento.dto.EmployeeOrderReportResponse;
import com.lovius.bento.exception.GlobalExceptionHandler;
import com.lovius.bento.security.AuthenticatedUser;
import com.lovius.bento.service.EmployeeOrderReportService;
import com.lovius.bento.service.TokenService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(A013Controller.class)
@Import(GlobalExceptionHandler.class)
class A013ControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EmployeeOrderReportService employeeOrderReportService;

    @MockBean
    private TokenService tokenService;

    @Test
    void orderReportsRequireAdmin() throws Exception {
        when(tokenService.parseToken("Bearer employee-token"))
                .thenReturn(new AuthenticatedUser(2L, "alice", "employee"));

        mockMvc.perform(get("/api/admin/reports/orders")
                        .header("Authorization", "Bearer employee-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.data.message").value("權限不足"));
    }

    @Test
    void adminCanQueryOrderReports() throws Exception {
        when(tokenService.parseToken("Bearer admin-token"))
                .thenReturn(new AuthenticatedUser(1L, "admin", "admin"));
        when(employeeOrderReportService.getReport(
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31),
                "department"))
                .thenReturn(List.of(new EmployeeOrderReportResponse(
                        LocalDate.of(2026, 3, 10),
                        "人資部",
                        "王小明",
                        "排骨便當",
                        "好好便當")));

        mockMvc.perform(get("/api/admin/reports/orders")
                        .header("Authorization", "Bearer admin-token")
                        .param("date_from", "2026-03-01")
                        .param("date_to", "2026-03-31")
                        .param("sort_by", "department"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].department_name").value("人資部"))
                .andExpect(jsonPath("$.data[0].supplier_name").value("好好便當"));
    }

    @Test
    void adminCanDownloadOrderReportPdf() throws Exception {
        when(tokenService.parseToken("Bearer admin-token"))
                .thenReturn(new AuthenticatedUser(1L, "admin", "admin"));
        when(employeeOrderReportService.downloadPdf(
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31),
                null))
                .thenReturn("%PDF-test".getBytes());

        mockMvc.perform(get("/api/admin/reports/orders/pdf")
                        .header("Authorization", "Bearer admin-token")
                        .param("date_from", "2026-03-01")
                        .param("date_to", "2026-03-31"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=employee-order-report.pdf"))
                .andExpect(content().contentType("application/pdf"))
                .andExpect(content().bytes("%PDF-test".getBytes()));
    }
}
