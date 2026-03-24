package com.lovius.bento.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lovius.bento.dao.MenuRepository;
import com.lovius.bento.dao.SupplierRepository;
import com.lovius.bento.dto.AdminSupplierResponse;
import com.lovius.bento.dto.SupplierResponse;
import com.lovius.bento.dto.UpdateSupplierRequest;
import com.lovius.bento.exception.ApiException;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SupplierServiceTest {
    @Mock
    private SupplierRepository supplierRepository;

    @Mock
    private MenuRepository menuRepository;

    private SupplierService supplierService;

    @BeforeEach
    void setUp() {
        supplierService = new SupplierService(supplierRepository, menuRepository);
    }

    @Test
    void getSuppliersReturnsAllWhenNameMissing() {
        when(supplierRepository.findAll()).thenReturn(List.of(supplier("好食便當")));

        List<SupplierResponse> response = supplierService.getSuppliers(null, null);

        assertEquals(1, response.size());
        assertEquals("好食便當", response.getFirst().name());
    }

    @Test
    void getSuppliersUsesFuzzySearchWhenRequested() {
        when(supplierRepository.findByNameFuzzy("好食")).thenReturn(List.of(supplier("好食便當")));

        List<SupplierResponse> response = supplierService.getSuppliers("好食", "fuzzy");

        assertEquals(1, response.size());
        verify(supplierRepository).findByNameFuzzy("好食");
    }

    @Test
    void getSuppliersRejectsUnsupportedSearchType() {
        ApiException exception = assertThrows(
                ApiException.class,
                () -> supplierService.getSuppliers("好食", "wild"));

        assertEquals("search_type 僅支援 exact 或 fuzzy", exception.getMessage());
    }

    @Test
    void updateSupplierPreservesReadonlyFields() {
        when(supplierRepository.findById(5L)).thenReturn(Optional.of(supplier("好食便當")));

        supplierService.updateSupplier(5L, new UpdateSupplierRequest(
                "新供應商名稱",
                "vendor@company.local",
                "02-8888-0000",
                "聯絡人",
                false,
                null,
                null));

        ArgumentCaptor<Supplier> captor = ArgumentCaptor.forClass(Supplier.class);
        verify(supplierRepository).save(captor.capture());
        assertEquals(5L, captor.getValue().getId());
        assertEquals("12345678", captor.getValue().getBusinessRegistrationNo());
        assertEquals("新供應商名稱", captor.getValue().getName());
    }

    @Test
    void getAdminSuppliersReturnsSuppliersWithMenuOptions() {
        when(supplierRepository.findAll()).thenReturn(List.of(supplier("好食便當")));
        when(menuRepository.findAll(true, LocalDate.now(), null)).thenReturn(List.of(menu(20L, 5L, "雞腿便當")));

        List<AdminSupplierResponse> response = supplierService.getAdminSuppliers();

        assertEquals(1, response.size());
        assertEquals("好食便當", response.getFirst().name());
        assertEquals(1, response.getFirst().menuOptions().size());
        assertEquals("雞腿便當", response.getFirst().menuOptions().getFirst().name());
    }

    private Supplier supplier(String name) {
        return new Supplier(
                5L,
                name,
                "orders@haoshi.local",
                "02-1234-5678",
                "王小明",
                "12345678",
                true,
                Instant.parse("2026-03-20T10:00:00Z"));
    }

    private Menu menu(Long id, Long supplierId, String name) {
        return new Menu(
                id,
                supplierId,
                name,
                "肉類",
                "附三樣配菜",
                new BigDecimal("120.00"),
                LocalDate.of(2026, 3, 30),
                LocalDate.of(2026, 4, 3),
                1L,
                Instant.parse("2026-03-20T10:00:00Z"),
                Instant.parse("2026-03-20T10:00:00Z"));
    }
}
