package com.lovius.bento.service;

import com.lovius.bento.dao.EmployeeRepository;
import com.lovius.bento.dao.OrderRepository;
import com.lovius.bento.dto.AdminOrderResponse;
import com.lovius.bento.dto.CreateAdminOrderRequest;
import com.lovius.bento.dto.CreateOrderRequest;
import com.lovius.bento.dto.OrderResponse;
import com.lovius.bento.dto.UpdateOrderRequest;
import com.lovius.bento.exception.ApiException;
import com.lovius.bento.model.AdminOrderView;
import com.lovius.bento.model.BentoOrder;
import com.lovius.bento.model.Employee;
import com.lovius.bento.model.Menu;
import com.lovius.bento.security.AuthenticatedUser;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class OrderService {
    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Taipei");
    private final OrderRepository orderRepository;
    private final EmployeeRepository employeeRepository;
    private final MenuService menuService;
    private final OrderDeadlineService orderDeadlineService;

    public OrderService(
            OrderRepository orderRepository,
            EmployeeRepository employeeRepository,
            MenuService menuService,
            OrderDeadlineService orderDeadlineService) {
        this.orderRepository = orderRepository;
        this.employeeRepository = employeeRepository;
        this.menuService = menuService;
        this.orderDeadlineService = orderDeadlineService;
    }

    public OrderResponse createOrReplaceOrder(AuthenticatedUser user, CreateOrderRequest request) {
        validateOrderDate(request.orderDate());
        Menu menu = validateMenuForDate(request.menuId(), request.orderDate());
        BentoOrder order = orderRepository.findByEmployeeIdAndOrderDate(user.employeeId(), request.orderDate())
                .map(existing -> {
                    existing.setMenuId(menu.getId());
                    return existing;
                })
                .orElseGet(() -> new BentoOrder(
                        null,
                        user.employeeId(),
                        menu.getId(),
                        request.orderDate(),
                        user.employeeId(),
                        Instant.now()));
        if (order.getId() != null) {
            order = new BentoOrder(
                    order.getId(),
                    order.getEmployeeId(),
                    menu.getId(),
                    order.getOrderDate(),
                    user.employeeId(),
                    Instant.now());
        }
        orderRepository.save(order);
        return toResponse(order);
    }

    public OrderResponse updateOrder(AuthenticatedUser user, Long orderId, UpdateOrderRequest request) {
        BentoOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "查無訂單"));
        ensureOwner(user, order);
        validateOrderDate(order.getOrderDate());
        Menu menu = validateMenuForDate(request.menuId(), order.getOrderDate());
        BentoOrder updated = new BentoOrder(
                order.getId(),
                order.getEmployeeId(),
                menu.getId(),
                order.getOrderDate(),
                user.employeeId(),
                Instant.now());
        orderRepository.save(updated);
        return toResponse(updated);
    }

    public void cancelOwnOrder(AuthenticatedUser user, Long orderId) {
        BentoOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "查無訂單"));
        ensureOwner(user, order);
        orderDeadlineService.ensureEmployeeCancellationWindowOpen(order.getOrderDate());
        orderRepository.deleteById(orderId);
    }

    public void cancelOrderByAdmin(Long orderId) {
        BentoOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "查無訂單"));
        orderDeadlineService.ensureAdminCancellationWindowOpen(order.getOrderDate());
        orderRepository.deleteById(orderId);
    }

    public List<OrderResponse> getMyOrders(AuthenticatedUser user) {
        return orderRepository.findByEmployeeId(user.employeeId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<AdminOrderResponse> getAdminOrders(LocalDate dateFrom, LocalDate dateTo, Long employeeId) {
        LocalDate resolvedDateFrom = dateFrom == null ? LocalDate.now(ZONE_ID) : dateFrom;
        LocalDate resolvedDateTo = dateTo == null ? resolvedDateFrom : dateTo;
        if (resolvedDateFrom.isAfter(resolvedDateTo)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "查詢起日不可晚於迄日");
        }
        return orderRepository.findAdminOrders(resolvedDateFrom, resolvedDateTo, employeeId)
                .stream()
                .map(this::toAdminResponse)
                .toList();
    }

    public OrderResponse createOrderByAdmin(AuthenticatedUser admin, CreateAdminOrderRequest request) {
        getRequiredEmployee(request.employeeId());
        orderDeadlineService.ensureAdminOrderDateIsTomorrow(request.orderDate());
        orderDeadlineService.ensureAdminOrderCreationWindowOpen(request.orderDate());
        Menu menu = validateMenuForDate(request.menuId(), request.orderDate());
        if (orderRepository.findByEmployeeIdAndOrderDate(request.employeeId(), request.orderDate()).isPresent()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "該員工於指定日期已有訂單");
        }

        BentoOrder order = new BentoOrder(
                null,
                request.employeeId(),
                menu.getId(),
                request.orderDate(),
                admin.employeeId(),
                Instant.now());
        orderRepository.save(order);
        return toResponse(order);
    }

    private void validateOrderDate(LocalDate orderDate) {
        orderDeadlineService.ensureEmployeeOrderableDate(orderDate);
    }

    private Menu validateMenuForDate(Long menuId, LocalDate orderDate) {
        Menu menu = menuService.getRequiredMenu(menuId);
        if (orderDate.isBefore(menu.getValidFrom()) || orderDate.isAfter(menu.getValidTo())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "菜單不適用於指定日期");
        }
        return menu;
    }

    private void ensureOwner(AuthenticatedUser user, BentoOrder order) {
        if (!user.employeeId().equals(order.getEmployeeId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "權限不足");
        }
    }

    private Employee getRequiredEmployee(Long employeeId) {
        return employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "查無員工"));
    }

    private OrderResponse toResponse(BentoOrder order) {
        Employee employee = getRequiredEmployee(order.getEmployeeId());
        Menu menu = menuService.getRequiredMenu(order.getMenuId());
        return new OrderResponse(
                order.getId(),
                employee.getId(),
                employee.getName(),
                menu.getId(),
                menu.getName(),
                order.getOrderDate(),
                order.getCreatedBy(),
                order.getCreatedAt());
    }

    private AdminOrderResponse toAdminResponse(AdminOrderView order) {
        return new AdminOrderResponse(
                order.id(),
                order.employeeId(),
                order.employeeName(),
                order.menuId(),
                order.menuName(),
                order.supplierId(),
                order.supplierName(),
                order.menuPrice(),
                order.orderDate(),
                order.createdBy(),
                order.createdByName(),
                order.createdAt());
    }
}
