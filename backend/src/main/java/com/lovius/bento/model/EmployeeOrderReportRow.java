package com.lovius.bento.model;

import java.time.LocalDate;

public record EmployeeOrderReportRow(
        LocalDate orderDate,
        String departmentName,
        String employeeName,
        String menuName,
        String supplierName) {}
