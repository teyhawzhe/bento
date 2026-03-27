package com.lovius.bento.dto;

import java.time.LocalDate;

public record EmployeeOrderReportResponse(
        LocalDate orderDate,
        String departmentName,
        String employeeName,
        String menuName,
        String supplierName) {}
