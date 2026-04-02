package com.lovius.bento.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lovius.bento.dao.MenuRepository;
import com.lovius.bento.dao.SupplierRepository;
import com.lovius.bento.model.Menu;
import com.lovius.bento.model.Supplier;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MenuServiceTest {
    @Mock
    private MenuRepository menuRepository;

    @Mock
    private SupplierRepository supplierRepository;

    @Mock
    private OrderDeadlineService orderDeadlineService;

    private MenuService menuService;

    @BeforeEach
    void setUp() {
        menuService = new MenuService(menuRepository, supplierRepository, orderDeadlineService);
    }

    @Test
    void getMenusDelegatesSupplierFilterToRepository() {
        when(menuRepository.findAll(false, LocalDate.now(), 5L)).thenReturn(List.of(menu(20L, 5L, "招牌便當")));

        var response = menuService.getMenus(5L);

        verify(menuRepository).findAll(false, LocalDate.now(), 5L);
        assertEquals(1, response.size());
        assertEquals(5L, response.getFirst().supplierId());
        assertEquals("招牌便當", response.getFirst().name());
    }

    @Test
    void toEmployeeResponseIncludesSupplierName() {
        when(supplierRepository.findById(5L)).thenReturn(Optional.of(new Supplier(
                5L,
                "好好店家",
                "orders@vendor.local",
                "02-1234-5678",
                "王小明",
                "12345678",
                true,
                Instant.parse("2026-03-20T10:00:00Z"))));

        var response = menuService.toEmployeeResponse(menu(20L, 5L, "招牌便當"));

        assertEquals("好好店家", response.supplierName());
        assertEquals("招牌便當", response.name());
    }

    private Menu menu(Long id, Long supplierId, String name) {
        return new Menu(
                id,
                supplierId,
                name,
                "主餐",
                "每日限量",
                new BigDecimal("120.00"),
                LocalDate.of(2026, 3, 24),
                LocalDate.of(2026, 3, 31),
                1L,
                Instant.parse("2026-03-23T08:30:00Z"),
                Instant.parse("2026-03-23T08:30:00Z"));
    }
}
