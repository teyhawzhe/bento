package com.lovius.bento.controller;

import com.lovius.bento.dto.AdminOrderResponse;
import com.lovius.bento.dto.CreateAdminOrderRequest;
import com.lovius.bento.dto.OrderResponse;
import com.lovius.bento.exception.GlobalExceptionHandler;
import com.lovius.bento.security.AuthenticatedUser;
import com.lovius.bento.service.MenuService;
import com.lovius.bento.service.OrderDeadlineService;
import com.lovius.bento.service.OrderService;
import com.lovius.bento.service.SupplierService;
import com.lovius.bento.service.TokenService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
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

@WebMvcTest(A002Controller.class)
@Import(GlobalExceptionHandler.class)
class A002ControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MenuService menuService;

    @MockBean
    private OrderService orderService;

    @MockBean
    private SupplierService supplierService;

    @MockBean
    private TokenService tokenService;

    @MockBean
    private OrderDeadlineService orderDeadlineService;

    @Test
    void getAdminOrdersRequiresAdmin() throws Exception {
        Mockito.when(tokenService.parseToken("Bearer employee-token"))
                .thenReturn(new AuthenticatedUser(2L, "alice", "employee"));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/admin/orders")
                        .header("Authorization", "Bearer employee-token"))
                .andExpect(MockMvcResultMatchers.status().isForbidden())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("權限不足"));
    }

    @Test
    void getAdminOrdersSupportsFilters() throws Exception {
        Mockito.when(tokenService.parseToken("Bearer admin-token"))
                .thenReturn(new AuthenticatedUser(1L, "admin", "admin"));
        Mockito.when(orderService.getAdminOrders(LocalDate.of(2026, 4, 1), 2L))
                .thenReturn(List.of(new AdminOrderResponse(
                        8L,
                        2L,
                        "Alice Chen",
                        20L,
                        "香烤雞腿便當",
                        5L,
                        "好食便當",
                        new BigDecimal("120.00"),
                        LocalDate.of(2026, 4, 1),
                        1L,
                        "System Admin",
                        Instant.parse("2026-03-31T08:30:00Z"))));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/admin/orders")
                        .header("Authorization", "Bearer admin-token")
                        .param("date", "2026-04-01")
                        .param("employee_id", "2"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].employeeName").value("Alice Chen"))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].supplierName").value("好食便當"))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].createdByName").value("System Admin"));
    }

    @Test
    void createAdminOrderDelegatesToService() throws Exception {
        AuthenticatedUser admin = new AuthenticatedUser(1L, "admin", "admin");
        CreateAdminOrderRequest request = new CreateAdminOrderRequest(2L, 20L, LocalDate.of(2026, 4, 1));
        Mockito.when(tokenService.parseToken("Bearer admin-token")).thenReturn(admin);
        Mockito.when(orderService.createOrderByAdmin(admin, request)).thenReturn(new OrderResponse(
                8L,
                2L,
                "Alice Chen",
                20L,
                "香烤雞腿便當",
                LocalDate.of(2026, 4, 1),
                1L,
                Instant.parse("2026-03-31T08:30:00Z")));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/admin/orders")
                        .header("Authorization", "Bearer admin-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "employeeId": 2,
                                  "menuId": 20,
                                  "orderDate": "2026-04-01"
                                }
                                """))
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andExpect(MockMvcResultMatchers.jsonPath("$.employeeId").value(2))
                .andExpect(MockMvcResultMatchers.jsonPath("$.createdBy").value(1));
    }
}
