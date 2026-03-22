package com.lovius.bento.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lovius.bento.dao.EmployeeRepository;
import com.lovius.bento.dao.OrderRepository;
import com.lovius.bento.dto.CreateAdminOrderRequest;
import com.lovius.bento.dto.CreateOrderRequest;
import com.lovius.bento.dto.OrderResponse;
import com.lovius.bento.exception.ApiException;
import com.lovius.bento.model.BentoOrder;
import com.lovius.bento.model.Employee;
import com.lovius.bento.model.Menu;
import com.lovius.bento.security.AuthenticatedUser;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {
    @Mock
    private OrderRepository orderRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private MenuService menuService;

    @Mock
    private OrderDeadlineService orderDeadlineService;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(orderRepository, employeeRepository, menuService, orderDeadlineService);
    }

    @Test
    void createOrReplaceOrderOverwritesExistingOrderForSameDate() {
        AuthenticatedUser user = new AuthenticatedUser(1L, "alice", "employee");
        LocalDate orderDate = LocalDate.of(2026, 3, 30);
        Menu menu = menu(20L, orderDate, orderDate.plusDays(4));
        BentoOrder existingOrder = new BentoOrder(99L, 1L, 10L, orderDate, 1L, Instant.parse("2026-03-24T01:00:00Z"));

        when(orderRepository.findByEmployeeIdAndOrderDate(1L, orderDate)).thenReturn(Optional.of(existingOrder));
        when(menuService.getRequiredMenu(20L)).thenReturn(menu);
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee()));

        OrderResponse response = orderService.createOrReplaceOrder(user, new CreateOrderRequest(20L, orderDate));

        ArgumentCaptor<BentoOrder> captor = ArgumentCaptor.forClass(BentoOrder.class);
        verify(orderRepository).save(captor.capture());
        assertEquals(99L, captor.getValue().getId());
        assertEquals(20L, captor.getValue().getMenuId());
        assertEquals("香烤雞腿便當", response.menuName());
    }

    @Test
    void createOrderRejectsDateOutsideCurrentOpenRange() {
        AuthenticatedUser user = new AuthenticatedUser(1L, "alice", "employee");
        LocalDate invalidDate = LocalDate.of(2026, 4, 10);

        doThrow(new ApiException(org.springframework.http.HttpStatus.BAD_REQUEST, "僅可訂購本次開放區間（截止日後至下週五）的便當"))
                .when(orderDeadlineService)
                .ensureEmployeeOrderableDate(invalidDate);

        ApiException exception = assertThrows(
                ApiException.class,
                () -> orderService.createOrReplaceOrder(user, new CreateOrderRequest(20L, invalidDate)));

        assertEquals("僅可訂購本次開放區間（截止日後至下週五）的便當", exception.getMessage());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void cancelOwnOrderRejectsOtherUsersOrder() {
        AuthenticatedUser user = new AuthenticatedUser(1L, "alice", "employee");
        BentoOrder order = new BentoOrder(99L, 2L, 10L, LocalDate.of(2026, 3, 30), 2L, Instant.now());

        when(orderRepository.findById(99L)).thenReturn(Optional.of(order));

        ApiException exception = assertThrows(ApiException.class, () -> orderService.cancelOwnOrder(user, 99L));

        assertEquals("權限不足", exception.getMessage());
        verify(orderRepository, never()).deleteById(any());
    }

    @Test
    void cancelOwnOrderUsesEmployeeCancellationDeadline() {
        AuthenticatedUser user = new AuthenticatedUser(1L, "alice", "employee");
        LocalDate orderDate = LocalDate.of(2026, 4, 1);
        BentoOrder order = new BentoOrder(99L, 1L, 10L, orderDate, 1L, Instant.now());

        when(orderRepository.findById(99L)).thenReturn(Optional.of(order));

        orderService.cancelOwnOrder(user, 99L);

        verify(orderDeadlineService).ensureEmployeeCancellationWindowOpen(orderDate);
        verify(orderRepository).deleteById(99L);
    }

    @Test
    void adminCancelUsesConfiguredOrderDateDeadline() {
        LocalDate orderDate = LocalDate.of(2026, 4, 1);
        BentoOrder order = new BentoOrder(88L, 1L, 10L, orderDate, 1L, Instant.now());

        when(orderRepository.findById(88L)).thenReturn(Optional.of(order));

        orderService.cancelOrderByAdmin(88L);

        verify(orderDeadlineService).ensureAdminCancellationWindowOpen(orderDate);
        verify(orderRepository).deleteById(88L);
    }

    @Test
    void updateOrderUsesEmployeeOrderableRangeValidation() {
        AuthenticatedUser user = new AuthenticatedUser(1L, "alice", "employee");
        LocalDate orderDate = LocalDate.of(2026, 3, 30);
        BentoOrder order = new BentoOrder(99L, 1L, 10L, orderDate, 1L, Instant.now());
        Menu menu = menu(20L, orderDate, orderDate.plusDays(2));

        when(orderRepository.findById(99L)).thenReturn(Optional.of(order));
        when(menuService.getRequiredMenu(20L)).thenReturn(menu);
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee()));

        orderService.updateOrder(user, 99L, new com.lovius.bento.dto.UpdateOrderRequest(20L));

        verify(orderDeadlineService).ensureEmployeeOrderableDate(orderDate);
        verify(orderRepository).save(any(BentoOrder.class));
    }

    @Test
    void createOrderByAdminPersistsAdminAsCreatedBy() {
        AuthenticatedUser admin = new AuthenticatedUser(9L, "admin", "admin");
        LocalDate orderDate = LocalDate.of(2026, 4, 1);
        Menu menu = menu(20L, orderDate, orderDate);

        when(employeeRepository.findById(2L)).thenReturn(Optional.of(employee(2L, "Alice Chen")));
        when(menuService.getRequiredMenu(20L)).thenReturn(menu);
        when(orderRepository.findByEmployeeIdAndOrderDate(2L, orderDate)).thenReturn(Optional.empty());

        OrderResponse response = orderService.createOrderByAdmin(admin, new CreateAdminOrderRequest(2L, 20L, orderDate));

        ArgumentCaptor<BentoOrder> captor = ArgumentCaptor.forClass(BentoOrder.class);
        verify(orderDeadlineService).ensureAdminOrderDateIsTomorrow(orderDate);
        verify(orderDeadlineService).ensureAdminOrderCreationWindowOpen(orderDate);
        verify(orderRepository).save(captor.capture());
        assertEquals(9L, captor.getValue().getCreatedBy());
        assertEquals(2L, captor.getValue().getEmployeeId());
        assertEquals(9L, response.createdBy());
    }

    @Test
    void createOrderByAdminRejectsExistingOrder() {
        AuthenticatedUser admin = new AuthenticatedUser(9L, "admin", "admin");
        LocalDate orderDate = LocalDate.of(2026, 4, 1);
        BentoOrder existingOrder = new BentoOrder(88L, 2L, 10L, orderDate, 2L, Instant.now());

        when(employeeRepository.findById(2L)).thenReturn(Optional.of(employee(2L, "Alice Chen")));
        when(orderRepository.findByEmployeeIdAndOrderDate(2L, orderDate)).thenReturn(Optional.of(existingOrder));
        when(menuService.getRequiredMenu(20L)).thenReturn(menu(20L, orderDate, orderDate));

        ApiException exception = assertThrows(
                ApiException.class,
                () -> orderService.createOrderByAdmin(admin, new CreateAdminOrderRequest(2L, 20L, orderDate)));

        assertEquals("該員工於指定日期已有訂單", exception.getMessage());
        verify(orderRepository, never()).save(any());
    }

    private Menu menu(Long id, LocalDate validFrom, LocalDate validTo) {
        return new Menu(
                id,
                5L,
                "香烤雞腿便當",
                "肉類",
                "附三樣配菜",
                new BigDecimal("120.00"),
                validFrom,
                validTo,
                9L,
                Instant.now(),
                Instant.now());
    }

    private Employee employee() {
        return employee(1L, "Alice Chen");
    }

    private Employee employee(Long id, String name) {
        return new Employee(
                id,
                "alice",
                "hash",
                name,
                "alice@company.local",
                false,
                true,
                Instant.now(),
                Instant.now());
    }
}
