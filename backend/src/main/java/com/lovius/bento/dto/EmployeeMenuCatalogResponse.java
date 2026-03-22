package com.lovius.bento.dto;

import java.time.LocalDate;
import java.util.List;

public record EmployeeMenuCatalogResponse(
        List<LocalDate> orderableDates,
        List<EmployeeMenuOptionResponse> menus) {}
