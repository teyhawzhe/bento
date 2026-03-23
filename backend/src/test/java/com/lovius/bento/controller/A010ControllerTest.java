package com.lovius.bento.controller;

import com.lovius.bento.dto.DepartmentSummaryResponse;
import com.lovius.bento.exception.GlobalExceptionHandler;
import com.lovius.bento.security.AuthenticatedUser;
import com.lovius.bento.service.DepartmentService;
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

@WebMvcTest(A010Controller.class)
@Import(GlobalExceptionHandler.class)
class A010ControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DepartmentService departmentService;

    @MockBean
    private TokenService tokenService;

    @Test
    void getDepartmentsRequiresAdmin() throws Exception {
        Mockito.when(tokenService.parseToken("Bearer employee-token"))
                .thenReturn(new AuthenticatedUser(2L, "alice", "employee"));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/admin/departments")
                        .header("Authorization", "Bearer employee-token"))
                .andExpect(MockMvcResultMatchers.status().isForbidden())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("權限不足"));
    }

    @Test
    void createDepartmentReturnsCreatedDepartment() throws Exception {
        Instant now = Instant.parse("2026-03-23T08:30:00Z");
        Mockito.when(tokenService.parseToken("Bearer admin-token"))
                .thenReturn(new AuthenticatedUser(1L, "admin", "admin"));
        Mockito.when(departmentService.createDepartment(Mockito.any()))
                .thenReturn(new DepartmentSummaryResponse(5L, "IT", true, now, now));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/admin/departments")
                        .header("Authorization", "Bearer admin-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "IT"
                                }
                                """))
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andExpect(MockMvcResultMatchers.jsonPath("$.name").value("IT"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.isActive").value(true));
    }

    @Test
    void getDepartmentsReturnsList() throws Exception {
        Instant now = Instant.parse("2026-03-23T08:30:00Z");
        Mockito.when(tokenService.parseToken("Bearer admin-token"))
                .thenReturn(new AuthenticatedUser(1L, "admin", "admin"));
        Mockito.when(departmentService.getAllDepartments()).thenReturn(List.of(
                new DepartmentSummaryResponse(1L, "Management", true, now, now)));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/admin/departments")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].name").value("Management"));
    }
}
