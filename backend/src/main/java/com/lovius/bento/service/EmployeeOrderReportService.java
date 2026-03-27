package com.lovius.bento.service;

import com.lovius.bento.dao.OrderRepository;
import com.lovius.bento.dto.EmployeeOrderReportResponse;
import com.lovius.bento.exception.ApiException;
import com.lovius.bento.model.EmployeeOrderReportRow;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class EmployeeOrderReportService {
    private static final List<String> SUPPORTED_SORTS = List.of("date", "department", "employee", "supplier");

    private final OrderRepository orderRepository;
    private final EmployeeOrderReportPdfService employeeOrderReportPdfService;

    public EmployeeOrderReportService(
            OrderRepository orderRepository,
            EmployeeOrderReportPdfService employeeOrderReportPdfService) {
        this.orderRepository = orderRepository;
        this.employeeOrderReportPdfService = employeeOrderReportPdfService;
    }

    public List<EmployeeOrderReportResponse> getReport(
            LocalDate dateFrom,
            LocalDate dateTo,
            String sortBy) {
        validateDateRange(dateFrom, dateTo);
        String normalizedSort = normalizeSort(sortBy);
        return orderRepository.findEmployeeOrderReportRows(dateFrom, dateTo, normalizedSort)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public byte[] downloadPdf(LocalDate dateFrom, LocalDate dateTo, String sortBy) {
        validateDateRange(dateFrom, dateTo);
        String normalizedSort = normalizeSort(sortBy);
        List<EmployeeOrderReportRow> rows = orderRepository.findEmployeeOrderReportRows(dateFrom, dateTo, normalizedSort);
        return employeeOrderReportPdfService.generatePdf(dateFrom, dateTo, normalizedSort, rows);
    }

    private void validateDateRange(LocalDate dateFrom, LocalDate dateTo) {
        if (dateFrom == null || dateTo == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "請完整提供查詢日期起訖");
        }
        if (dateFrom.isAfter(dateTo)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "查詢起日不可晚於迄日");
        }
    }

    private String normalizeSort(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) {
            return "date";
        }
        if (!SUPPORTED_SORTS.contains(sortBy)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "不支援的排序條件");
        }
        return sortBy;
    }

    private EmployeeOrderReportResponse toResponse(EmployeeOrderReportRow row) {
        return new EmployeeOrderReportResponse(
                row.orderDate(),
                row.departmentName(),
                row.employeeName(),
                row.menuName(),
                row.supplierName());
    }
}
