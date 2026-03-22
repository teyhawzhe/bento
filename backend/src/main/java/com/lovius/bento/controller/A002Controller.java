package com.lovius.bento.controller;

import com.lovius.bento.dto.ApiMessageResponse;
import com.lovius.bento.dto.AdminOrderResponse;
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
import com.lovius.bento.service.OrderDeadlineService;
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
    private final OrderDeadlineService orderDeadlineService;

    public A002Controller(
            MenuService menuService,
            OrderService orderService,
            SupplierService supplierService,
            TokenService tokenService,
            OrderDeadlineService orderDeadlineService) {
        this.menuService = menuService;
        this.orderService = orderService;
        this.supplierService = supplierService;
        this.tokenService = tokenService;
        this.orderDeadlineService = orderDeadlineService;
    }

    @GetMapping("/orders/menu")
    public ResponseEntity<?> getNextWeekMenus(
            @RequestHeader("Authorization") String authorizationHeader) {
        requireRole(authorizationHeader, "employee");
        if (!orderDeadlineService.isEmployeeOrderWindowOpen()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiMessageResponse("本週訂餐已截止，僅可查看個人訂餐記錄"));
        }
        List<EmployeeMenuOptionResponse> menus = menuService.getNextWeekMenusForEmployee();
        return ResponseEntity.ok(menus);
    }

    @PostMapping("/orders")
    public ResponseEntity<OrderResponse> createOrder(
            @RequestHeader("Authorization") String authorizationHeader,
            @Valid @RequestBody CreateOrderRequest request) {
        AuthenticatedUser user = requireRole(authorizationHeader, "employee");
        OrderResponse response = orderService.createOrReplaceOrder(user, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/orders/{id}")
    public OrderResponse updateOrder(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable("id") Long orderId,
            @Valid @RequestBody UpdateOrderRequest request) {
        AuthenticatedUser user = requireRole(authorizationHeader, "employee");
        return orderService.updateOrder(user, orderId, request);
    }

    @DeleteMapping("/orders/{id}")
    public ApiMessageResponse cancelOrder(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable("id") Long orderId) {
        AuthenticatedUser user = requireRole(authorizationHeader, "employee");
        orderService.cancelOwnOrder(user, orderId);
        return new ApiMessageResponse("訂餐已取消");
    }

    @GetMapping("/orders/me")
    public List<OrderResponse> getMyOrders(
            @RequestHeader("Authorization") String authorizationHeader) {
        AuthenticatedUser user = requireRole(authorizationHeader, "employee");
        return orderService.getMyOrders(user);
    }

    @GetMapping("/menus")
    public List<MenuResponse> getMenus(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestParam(name = "include_history", defaultValue = "false") boolean includeHistory) {
        requireRole(authorizationHeader, "admin");
        return menuService.getMenus(includeHistory);
    }

    @PostMapping("/menus")
    public ResponseEntity<MenuResponse> createMenu(
            @RequestHeader("Authorization") String authorizationHeader,
            @Valid @RequestBody CreateMenuRequest request) {
        AuthenticatedUser user = requireRole(authorizationHeader, "admin");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(menuService.createMenu(user.employeeId(), request));
    }

    @PatchMapping("/menus/{id}")
    public MenuResponse updateMenu(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable("id") Long menuId,
            @Valid @RequestBody UpdateMenuRequest request) {
        requireRole(authorizationHeader, "admin");
        return menuService.updateMenu(menuId, request);
    }

    @PostMapping("/suppliers")
    public ResponseEntity<SupplierResponse> createSupplier(
            @RequestHeader("Authorization") String authorizationHeader,
            @Valid @RequestBody CreateSupplierRequest request) {
        requireRole(authorizationHeader, "admin");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(supplierService.createSupplier(request));
    }

    @GetMapping("/suppliers")
    public List<SupplierResponse> getSuppliers(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestParam(name = "name", required = false) String name,
            @RequestParam(name = "search_type", required = false) String searchType) {
        requireRole(authorizationHeader, "admin");
        return supplierService.getSuppliers(name, searchType);
    }

    @GetMapping("/suppliers/{id}")
    public SupplierResponse getSupplier(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable("id") Long supplierId) {
        requireRole(authorizationHeader, "admin");
        return supplierService.getSupplier(supplierId);
    }

    @PatchMapping("/suppliers/{id}")
    public SupplierResponse updateSupplier(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable("id") Long supplierId,
            @Valid @RequestBody UpdateSupplierRequest request) {
        requireRole(authorizationHeader, "admin");
        return supplierService.updateSupplier(supplierId, request);
    }

    @DeleteMapping("/admin/orders/{id}")
    public ApiMessageResponse cancelOrderByAdmin(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable("id") Long orderId) {
        requireRole(authorizationHeader, "admin");
        orderService.cancelOrderByAdmin(orderId);
        return new ApiMessageResponse("已取消指定員工訂餐");
    }

    @GetMapping("/admin/orders")
    public List<AdminOrderResponse> getOrdersByAdmin(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestParam(name = "date", required = false) LocalDate orderDate,
            @RequestParam(name = "employee_id", required = false) Long employeeId) {
        requireRole(authorizationHeader, "admin");
        return orderService.getAdminOrders(orderDate, employeeId);
    }

    @PostMapping("/admin/orders")
    public ResponseEntity<OrderResponse> createOrderByAdmin(
            @RequestHeader("Authorization") String authorizationHeader,
            @Valid @RequestBody CreateAdminOrderRequest request) {
        AuthenticatedUser user = requireRole(authorizationHeader, "admin");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderService.createOrderByAdmin(user, request));
    }

    private AuthenticatedUser requireRole(String authorizationHeader, String expectedRole) {
        AuthenticatedUser authenticatedUser = tokenService.parseToken(authorizationHeader);
        if (!expectedRole.equals(authenticatedUser.role())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "權限不足");
        }
        return authenticatedUser;
    }
}
