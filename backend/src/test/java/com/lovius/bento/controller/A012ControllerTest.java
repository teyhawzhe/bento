package com.lovius.bento.controller;

import com.lovius.bento.dto.ImportDepartmentResult;
import com.lovius.bento.dto.ImportEmployeeResult;
import com.lovius.bento.exception.CsvImportException;
import com.lovius.bento.exception.GlobalExceptionHandler;
import com.lovius.bento.security.AuthenticatedUser;
import com.lovius.bento.service.CsvImportService;
import com.lovius.bento.service.TokenService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@WebMvcTest(A012Controller.class)
@Import(GlobalExceptionHandler.class)
class A012ControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CsvImportService csvImportService;

    @MockBean
    private TokenService tokenService;

    @Test
    void downloadTemplateReturnsCsvContent() throws Exception {
        Mockito.when(tokenService.parseToken("Bearer admin-token"))
                .thenReturn(new AuthenticatedUser(1L, "admin", "admin"));
        Mockito.when(csvImportService.getTemplate("employees"))
                .thenReturn("username,name,email,department_id\n");

        mockMvc.perform(MockMvcRequestBuilders.get("/api/admin/import/template/employees")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().string("username,name,email,department_id\n"))
                .andExpect(MockMvcResultMatchers.header().string("Content-Disposition", "attachment; filename=\"employees-template.csv\""));
    }

    @Test
    void importEmployeesReturnsImportedRows() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "employees.csv", "text/csv", "x".getBytes());
        Mockito.when(tokenService.parseToken("Bearer admin-token"))
                .thenReturn(new AuthenticatedUser(1L, "admin", "admin"));
        Mockito.when(csvImportService.importEmployees(Mockito.any())).thenReturn(List.of(
                new ImportEmployeeResult(5L, "alice", "Alice", "alice@company.local", 2L, false, true)));

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/admin/import/employees")
                        .file(file)
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.data[0].department_id").value(2))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data[0].is_admin").value(false));
    }

    @Test
    void importDepartmentsReturns422ForBatchFailure() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "departments.csv", "text/csv", "x".getBytes());
        Mockito.when(tokenService.parseToken("Bearer admin-token"))
                .thenReturn(new AuthenticatedUser(1L, "admin", "admin"));
        Mockito.when(csvImportService.importDepartments(Mockito.any()))
                .thenThrow(new CsvImportException(
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "第 3 行驗證失敗，已 Rollback，請手動刪除已處理列後重新上傳",
                        3,
                        "部門名稱已存在"));

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/admin/import/departments")
                        .file(file)
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(MockMvcResultMatchers.status().isUnprocessableEntity())
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value("failed"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.failed_at_line").value(3))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.reason").value("部門名稱已存在"));
    }
}
