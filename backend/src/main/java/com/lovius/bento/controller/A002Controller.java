package com.lovius.bento.controller;

import com.lovius.bento.dto.ApiMessageResponse;
import com.lovius.bento.dto.ApiSuccessResponse;
import com.lovius.bento.dto.AdminSupplierResponse;
import com.lovius.bento.dto.CreateAdminOrderRequest;
import com.lovius.bento.dto.CreateMenuRequest;
import com.lovius.bento.dto.CreateOrderRequest;
import com.lovius.bento.dto.CreateSupplierRequest;
import com.lovius.bento.dto.EmployeeMenuOptionResponse;
import com.lovius.bento.dto.MenuResponse;
import com.lovius.bento.dto.OrderResponse;
import com.lovius.bento.dto.SupplierResponse;
import com.lovius.bento.dto.UpdateSupplierRequest;
import com.lovius.bento.dto.UpdateMenuRequest;
import com.lovius.bento.dto.UpdateOrderRequest;
import com.lovius.bento.exception.ApiException;
import com.lovius.bento.security.AuthenticatedUser;
import com.lovius.bento.service.MenuService;
import com.lovius.bento.service.OrderService;
import com.lovius.bento.service.SupplierService;
import com.lovius.bento.service.TokenService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api")
public class A002Controller {
    private final MenuService menuService;
    private final OrderService orderService;
    private final SupplierService supplierService;
    private final TokenService tokenService;

    public A002Controller(
            MenuService menuService,
            OrderService orderService,
            SupplierService supplierService,
            TokenService tokenService) {
        this.menuService = menuService;
        this.orderService = orderService;
        this.supplierService = supplierService;
        this.tokenService = tokenService;
    }

    @GetMapping("/orders/menu")
    public ApiSuccessResponse<List<EmployeeMenuOptionResponse>> getNextWeekMenus(
            @RequestHeader("Authorization") String authorizationHeader) {
        requireRole(authorizationHeader, "employee");
        return ApiSuccessResponse.success(menuService.getEmployeeMenusForEmployee());
    }

    @PostMapping("/orders")
    public ResponseEntity<ApiSuccessResponse<OrderResponse>> createOrder(
            @RequestHeader("Authorization") String authorizationHeader,
            @Valid @RequestBody CreateOrderRequest request) {
        AuthenticatedUser user = requireRole(authorizationHeader, "employee");
        OrderResponse response = orderService.createOrReplaceOrder(user, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiSuccessResponse.success(response));
    }

    @PatchMapping("/orders/{id}")
    public ApiSuccessResponse<OrderResponse> updateOrder(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable("id") Long orderId,
            @Valid @RequestBody UpdateOrderRequest request) {
        AuthenticatedUser user = requireRole(authorizationHeader, "employee");
        return ApiSuccessResponse.success(orderService.updateOrder(user, orderId, request));
    }

    @DeleteMapping("/orders/{id}")
    public ApiSuccessResponse<Void> cancelOrder(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable("id") Long orderId) {
        AuthenticatedUser user = requireRole(authorizationHeader, "employee");
        orderService.cancelOwnOrder(user, orderId);
        return ApiSuccessResponse.empty();
    }

    @GetMapping("/orders/me")
    public ApiSuccessResponse<List<OrderResponse>> getMyOrders(
            @RequestHeader("Authorization") String authorizationHeader) {
        AuthenticatedUser user = requireRole(authorizationHeader, "employee");
        return ApiSuccessResponse.success(orderService.getMyOrders(user));
    }

    @GetMapping("/menus")
    public ApiSuccessResponse<List<MenuResponse>> getMenus(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestParam(name = "supplier_id", required = false) Long supplierId) {
        requireRole(authorizationHeader, "admin");
        return ApiSuccessResponse.success(menuService.getMenus(supplierId));
    }

    @PostMapping("/menus")
    public ResponseEntity<ApiSuccessResponse<MenuResponse>> createMenu(
            @RequestHeader("Authorization") String authorizationHeader,
            @Valid @RequestBody CreateMenuRequest request) {
        AuthenticatedUser user = requireRole(authorizationHeader, "admin");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiSuccessResponse.success(menuService.createMenu(user.employeeId(), request)));
    }

    @PatchMapping("/menus/{id}")
    public ApiSuccessResponse<MenuResponse> updateMenu(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable("id") Long menuId,
            @Valid @RequestBody UpdateMenuRequest request) {
        requireRole(authorizationHeader, "admin");
        return ApiSuccessResponse.success(menuService.updateMenu(menuId, request));
    }

    @PostMapping("/suppliers")
    public ResponseEntity<ApiSuccessResponse<SupplierResponse>> createSupplier(
            @RequestHeader("Authorization") String authorizationHeader,
            @Valid @RequestBody CreateSupplierRequest request) {
        requireRole(authorizationHeader, "admin");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiSuccessResponse.success(supplierService.createSupplier(request)));
    }

    @GetMapping("/suppliers")
    public ApiSuccessResponse<List<SupplierResponse>> getSuppliers(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestParam(name = "name", required = false) String name,
            @RequestParam(name = "search_type", required = false) String searchType) {
        requireRole(authorizationHeader, "admin");
        return ApiSuccessResponse.success(supplierService.getSuppliers(name, searchType));
    }

    @GetMapping("/admin/suppliers")
    public ApiSuccessResponse<List<AdminSupplierResponse>> getAdminSuppliers(
            @RequestHeader("Authorization") String authorizationHeader) {
        requireRole(authorizationHeader, "admin");
        return ApiSuccessResponse.success(supplierService.getAdminSuppliers());
    }

    @GetMapping("/suppliers/{id}")
    public ApiSuccessResponse<SupplierResponse> getSupplier(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable("id") Long supplierId) {
        requireRole(authorizationHeader, "admin");
        return ApiSuccessResponse.success(supplierService.getSupplier(supplierId));
    }

    @PatchMapping("/suppliers/{id}")
    public ApiSuccessResponse<SupplierResponse> updateSupplier(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable("id") Long supplierId,
            @Valid @RequestBody UpdateSupplierRequest request) {
        requireRole(authorizationHeader, "admin");
        return ApiSuccessResponse.success(supplierService.updateSupplier(supplierId, request));
    }

    @DeleteMapping("/admin/orders/{id}")
    public ApiSuccessResponse<Void> cancelOrderByAdmin(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable("id") Long orderId) {
        requireRole(authorizationHeader, "admin");
        orderService.cancelOrderByAdmin(orderId);
        return ApiSuccessResponse.empty();
    }

    @GetMapping("/admin/orders")
    public ApiSuccessResponse<List<OrderResponse>> getOrdersByAdmin(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestParam(name = "date_from", required = false) LocalDate dateFrom,
            @RequestParam(name = "date_to", required = false) LocalDate dateTo,
            @RequestParam(name = "employee_id", required = false) Long employeeId) {
        requireRole(authorizationHeader, "admin");
        return ApiSuccessResponse.success(orderService.getAdminOrders(dateFrom, dateTo, employeeId));
    }

    @PostMapping("/admin/orders")
    public ResponseEntity<ApiSuccessResponse<OrderResponse>> createOrderByAdmin(
            @RequestHeader("Authorization") String authorizationHeader,
            @Valid @RequestBody CreateAdminOrderRequest request) {
        AuthenticatedUser user = requireRole(authorizationHeader, "admin");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiSuccessResponse.success(orderService.createOrderByAdmin(user, request)));
    }

    private AuthenticatedUser requireRole(String authorizationHeader, String expectedRole) {
        AuthenticatedUser authenticatedUser = tokenService.parseToken(authorizationHeader);
        if (!expectedRole.equals(authenticatedUser.role())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "權限不足");
        }
        return authenticatedUser;
    }
}
