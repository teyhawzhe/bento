package com.lovius.bento.service;

import com.lovius.bento.dao.MenuRepository;
import com.lovius.bento.dao.SupplierRepository;
import com.lovius.bento.dto.CreateMenuRequest;
import com.lovius.bento.dto.EmployeeMenuOptionResponse;
import com.lovius.bento.dto.MenuResponse;
import com.lovius.bento.dto.UpdateMenuRequest;
import com.lovius.bento.exception.ApiException;
import com.lovius.bento.model.Menu;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class MenuService {
    private final MenuRepository menuRepository;
    private final SupplierRepository supplierRepository;
    private final OrderDeadlineService orderDeadlineService;

    public MenuService(
            MenuRepository menuRepository,
            SupplierRepository supplierRepository,
            OrderDeadlineService orderDeadlineService) {
        this.menuRepository = menuRepository;
        this.supplierRepository = supplierRepository;
        this.orderDeadlineService = orderDeadlineService;
    }

    public List<EmployeeMenuOptionResponse> getEmployeeMenusForEmployee() {
        LinkedHashMap<Long, Menu> uniqueMenus = new LinkedHashMap<>();
        for (LocalDate date : orderDeadlineService.employeeOrderableDates()) {
            List<Menu> availableMenus = menuRepository.findAvailableForDate(date);
            if (availableMenus.isEmpty()) {
                continue;
            }
            availableMenus.forEach(menu -> uniqueMenus.putIfAbsent(menu.getId(), menu));
        }
        return uniqueMenus.values()
                .stream()
                .map(this::toEmployeeResponse)
                .toList();
    }

    public List<MenuResponse> getMenus(Long supplierId) {
        return menuRepository.findAll(false, LocalDate.now(), supplierId)
                .stream()
                .map(this::toAdminResponse)
                .toList();
    }

    public MenuResponse createMenu(Long createdBy, CreateMenuRequest request) {
        validateSupplier(request.supplierId());
        validateDateRange(request.validFrom(), request.validTo());
        Instant now = Instant.now();
        Menu menu = new Menu(
                null,
                request.supplierId(),
                request.name().trim(),
                request.category().trim(),
                normalizeDescription(request.description()),
                request.price(),
                request.validFrom(),
                request.validTo(),
                createdBy,
                now,
                now);
        menuRepository.save(menu);
        return toAdminResponse(menu);
    }

    public MenuResponse updateMenu(Long menuId, UpdateMenuRequest request) {
        Menu menu = menuRepository.findById(menuId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "查無菜單"));

        if (request.supplierId() != null) {
            validateSupplier(request.supplierId());
            menu.setSupplierId(request.supplierId());
        }
        if (request.name() != null && !request.name().isBlank()) {
            menu.setName(request.name().trim());
        }
        if (request.category() != null && !request.category().isBlank()) {
            menu.setCategory(request.category().trim());
        }
        if (request.description() != null) {
            menu.setDescription(normalizeDescription(request.description()));
        }
        if (request.price() != null) {
            menu.setPrice(request.price());
        }
        LocalDate nextValidFrom = request.validFrom() != null ? request.validFrom() : menu.getValidFrom();
        LocalDate nextValidTo = request.validTo() != null ? request.validTo() : menu.getValidTo();
        validateDateRange(nextValidFrom, nextValidTo);
        menu.setValidFrom(nextValidFrom);
        menu.setValidTo(nextValidTo);
        menu.touchUpdatedAt(Instant.now());
        menuRepository.save(menu);
        return toAdminResponse(menu);
    }

    public Menu getRequiredMenu(Long menuId) {
        return menuRepository.findById(menuId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "查無菜單"));
    }

    public EmployeeMenuOptionResponse toEmployeeResponse(Menu menu) {
        return new EmployeeMenuOptionResponse(
                menu.getId(),
                menu.getName(),
                menu.getCategory(),
                menu.getDescription(),
                menu.getValidFrom(),
                menu.getValidTo());
    }

    public MenuResponse toAdminResponse(Menu menu) {
        return new MenuResponse(
                menu.getId(),
                menu.getSupplierId(),
                menu.getName(),
                menu.getCategory(),
                menu.getDescription(),
                menu.getPrice(),
                menu.getValidFrom(),
                menu.getValidTo(),
                menu.getCreatedBy(),
                menu.getCreatedAt(),
                menu.getUpdatedAt());
    }

    private void validateSupplier(Long supplierId) {
        supplierRepository.findById(supplierId)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "供應商不存在"));
    }

    private void validateDateRange(LocalDate validFrom, LocalDate validTo) {
        if (validFrom.isAfter(validTo)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "菜單有效期間設定無效");
        }
    }

    private String normalizeDescription(String description) {
        return description == null ? "" : description.trim();
    }
}
