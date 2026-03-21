package com.lovius.bento.service;

import com.lovius.bento.dao.EmployeeRepository;
import com.lovius.bento.dao.OrderRepository;
import com.lovius.bento.dto.CreateOrderRequest;
import com.lovius.bento.dto.OrderResponse;
import com.lovius.bento.dto.UpdateOrderRequest;
import com.lovius.bento.exception.ApiException;
import com.lovius.bento.model.BentoOrder;
import com.lovius.bento.model.Employee;
import com.lovius.bento.model.Menu;
import com.lovius.bento.security.AuthenticatedUser;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class OrderService {
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
        orderDeadlineService.ensureEmployeeOrderWindowOpen();
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
        orderDeadlineService.ensureEmployeeOrderWindowOpen();
        BentoOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "查無訂單"));
        ensureOwner(user, order);
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
        orderDeadlineService.ensureEmployeeOrderWindowOpen();
        BentoOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "查無訂單"));
        ensureOwner(user, order);
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

    private void validateOrderDate(LocalDate orderDate) {
        if (!orderDeadlineService.isOrderDateWithinNextWeekdays(orderDate)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "僅可訂購下週工作日便當");
        }
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

    private OrderResponse toResponse(BentoOrder order) {
        Employee employee = employeeRepository.findById(order.getEmployeeId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "查無員工"));
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
}
