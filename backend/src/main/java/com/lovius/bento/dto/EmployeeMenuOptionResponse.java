package com.lovius.bento.dto;

import java.time.LocalDate;

public record EmployeeMenuOptionResponse(
        Long id,
        String name,
        String category,
        String description,
        LocalDate validFrom,
        LocalDate validTo) {}
