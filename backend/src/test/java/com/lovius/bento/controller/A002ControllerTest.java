package com.lovius.bento.controller;

import com.lovius.bento.dto.AdminOrderResponse;
import com.lovius.bento.dto.CreateAdminOrderRequest;
import com.lovius.bento.dto.EmployeeMenuCatalogResponse;
import com.lovius.bento.dto.EmployeeMenuOptionResponse;
import com.lovius.bento.dto.OrderResponse;
import com.lovius.bento.dto.SupplierResponse;
import com.lovius.bento.exception.GlobalExceptionHandler;
import com.lovius.bento.security.AuthenticatedUser;
import com.lovius.bento.service.MenuService;
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

    @Test
    void getEmployeeMenusReturnsOrderableDatesAndMenus() throws Exception {
        Mockito.when(tokenService.parseToken("Bearer employee-token"))
                .thenReturn(new AuthenticatedUser(2L, "alice", "employee"));
        Mockito.when(menuService.getEmployeeMenuCatalogForEmployee()).thenReturn(new EmployeeMenuCatalogResponse(
                List.of(LocalDate.of(2026, 3, 24), LocalDate.of(2026, 3, 25)),
                List.of(new EmployeeMenuOptionResponse(
                        20L,
                        "香烤雞腿便當",
                        "肉類",
                        "附三樣配菜",
                        LocalDate.of(2026, 3, 24),
                        LocalDate.of(2026, 3, 31)))));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/orders/menu")
                        .header("Authorization", "Bearer employee-token"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.orderableDates[0]").value("2026-03-24"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.menus[0].name").value("香烤雞腿便當"));
    }

    @Test
    void getMenusSupportsSupplierFilter() throws Exception {
        Mockito.when(tokenService.parseToken("Bearer admin-token"))
                .thenReturn(new AuthenticatedUser(1L, "admin", "admin"));
        Mockito.when(menuService.getMenus(true, 5L)).thenReturn(List.of(new com.lovius.bento.dto.MenuResponse(
                20L,
                5L,
                "招牌便當",
                "主餐",
                "每日限量",
                new BigDecimal("120.00"),
                LocalDate.of(2026, 3, 24),
                LocalDate.of(2026, 3, 31),
                1L,
                Instant.parse("2026-03-23T08:30:00Z"),
                Instant.parse("2026-03-23T08:30:00Z"))));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/menus")
                        .header("Authorization", "Bearer admin-token")
                        .param("include_history", "true")
                        .param("supplier_id", "5"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].supplierId").value(5))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].name").value("招牌便當"));
    }

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
        Mockito.when(orderService.getAdminOrders(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 3), 2L))
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
                        .param("date_from", "2026-04-01")
                        .param("date_to", "2026-04-03")
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

    @Test
    void getSuppliersRequiresAdmin() throws Exception {
        Mockito.when(tokenService.parseToken("Bearer employee-token"))
                .thenReturn(new AuthenticatedUser(2L, "alice", "employee"));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/suppliers")
                        .header("Authorization", "Bearer employee-token"))
                .andExpect(MockMvcResultMatchers.status().isForbidden())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("權限不足"));
    }

    @Test
    void getSuppliersSupportsSearchParams() throws Exception {
        Mockito.when(tokenService.parseToken("Bearer admin-token"))
                .thenReturn(new AuthenticatedUser(1L, "admin", "admin"));
        Mockito.when(supplierService.getSuppliers("好食", "fuzzy")).thenReturn(List.of(
                new SupplierResponse(
                        5L,
                        "好食便當",
                        "orders@haoshi.local",
                        "02-1234-5678",
                        "王小明",
                        "12345678",
                        true,
                        Instant.parse("2026-03-20T10:00:00Z"))));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/suppliers")
                        .header("Authorization", "Bearer admin-token")
                        .param("name", "好食")
                        .param("search_type", "fuzzy"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].name").value("好食便當"));
    }

    @Test
    void getSingleSupplierDelegatesToService() throws Exception {
        Mockito.when(tokenService.parseToken("Bearer admin-token"))
                .thenReturn(new AuthenticatedUser(1L, "admin", "admin"));
        Mockito.when(supplierService.getSupplier(5L)).thenReturn(new SupplierResponse(
                5L,
                "好食便當",
                "orders@haoshi.local",
                "02-1234-5678",
                "王小明",
                "12345678",
                true,
                Instant.parse("2026-03-20T10:00:00Z")));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/suppliers/5")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.businessRegistrationNo").value("12345678"));
    }

    @Test
    void patchSupplierRejectsReadonlyFields() throws Exception {
        Mockito.when(tokenService.parseToken("Bearer admin-token"))
                .thenReturn(new AuthenticatedUser(1L, "admin", "admin"));

        mockMvc.perform(MockMvcRequestBuilders.patch("/api/suppliers/5")
                        .header("Authorization", "Bearer admin-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "新供應商名稱",
                                  "email": "vendor@company.local",
                                  "phone": "02-8888-0000",
                                  "contactPerson": "聯絡人",
                                  "isActive": true,
                                  "businessRegistrationNo": "99999999"
                                }
                                """))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("營業登記編號不可修改"));
    }

    @Test
    void patchSupplierDelegatesToService() throws Exception {
        Mockito.when(tokenService.parseToken("Bearer admin-token"))
                .thenReturn(new AuthenticatedUser(1L, "admin", "admin"));
        Mockito.when(supplierService.updateSupplier(Mockito.eq(5L), Mockito.any())).thenReturn(new SupplierResponse(
                5L,
                "新供應商名稱",
                "vendor@company.local",
                "02-8888-0000",
                "聯絡人",
                "12345678",
                false,
                Instant.parse("2026-03-20T10:00:00Z")));

        mockMvc.perform(MockMvcRequestBuilders.patch("/api/suppliers/5")
                        .header("Authorization", "Bearer admin-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "新供應商名稱",
                                  "email": "vendor@company.local",
                                  "phone": "02-8888-0000",
                                  "contactPerson": "聯絡人",
                                  "isActive": false
                                }
                                """))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.name").value("新供應商名稱"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.isActive").value(false));
    }
}
