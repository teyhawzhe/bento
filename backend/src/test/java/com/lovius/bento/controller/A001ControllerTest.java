package com.lovius.bento.controller;

import com.lovius.bento.dto.DepartmentSummaryResponse;
import com.lovius.bento.dto.EmployeeCreatedResponse;
import com.lovius.bento.dto.EmployeeSummaryResponse;
import com.lovius.bento.dto.UpdateEmployeeResponse;
import com.lovius.bento.exception.GlobalExceptionHandler;
import com.lovius.bento.security.AuthenticatedUser;
import com.lovius.bento.service.AuthService;
import com.lovius.bento.service.EmployeeService;
import com.lovius.bento.service.TokenService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@WebMvcTest(A001Controller.class)
@Import(GlobalExceptionHandler.class)
class A001ControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private EmployeeService employeeService;

    @MockBean
    private TokenService tokenService;

    @Test
    void getEmployeesReturnsDepartmentFields() throws Exception {
        Mockito.when(tokenService.parseToken("Bearer admin-token"))
                .thenReturn(new AuthenticatedUser(1L, "admin", "admin"));
        Mockito.when(employeeService.getAllEmployees()).thenReturn(List.of(employeeSummary()));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/admin/employees")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].department.id").value(2))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].department.name").value("Operations"));
    }

    @Test
    void createEmployeeAcceptsDepartmentId() throws Exception {
        Mockito.when(tokenService.parseToken("Bearer admin-token"))
                .thenReturn(new AuthenticatedUser(1L, "admin", "admin"));
        Mockito.when(employeeService.createEmployee(Mockito.any())).thenReturn(
                new EmployeeCreatedResponse("員工帳號建立成功", employeeSummary(), "WelcomeA1"));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/admin/employees")
                        .header("Authorization", "Bearer admin-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "alice",
                                  "name": "Alice Chen",
                                  "email": "alice@company.local",
                                  "departmentId": 2
                                }
                                """))
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andExpect(MockMvcResultMatchers.jsonPath("$.employee.department.name").value("Operations"));
    }

    @Test
    void updateEmployeeAcceptsEditableFields() throws Exception {
        Mockito.when(tokenService.parseToken("Bearer admin-token"))
                .thenReturn(new AuthenticatedUser(1L, "admin", "admin"));
        Mockito.when(employeeService.updateEmployee(Mockito.eq(2L), Mockito.any()))
                .thenReturn(new UpdateEmployeeResponse("員工資料已更新", employeeSummary()));

        mockMvc.perform(MockMvcRequestBuilders.patch("/api/admin/employees/2")
                        .header("Authorization", "Bearer admin-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "alice",
                                  "name": "Alice Chen",
                                  "email": "alice@company.local",
                                  "departmentId": 2,
                                  "isAdmin": true
                                }
                                """))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.employee.id").value(2))
                .andExpect(MockMvcResultMatchers.jsonPath("$.employee.isAdmin").value(false))
                .andExpect(MockMvcResultMatchers.jsonPath("$.employee.department.name").value("Operations"));
    }

    private EmployeeSummaryResponse employeeSummary() {
        Instant now = Instant.parse("2026-03-23T08:30:00Z");
        return new EmployeeSummaryResponse(
                2L,
                "alice",
                "Alice Chen",
                "alice@company.local",
                new DepartmentSummaryResponse(2L, "Operations", true, now, now),
                false,
                true,
                now,
                now);
    }
}
