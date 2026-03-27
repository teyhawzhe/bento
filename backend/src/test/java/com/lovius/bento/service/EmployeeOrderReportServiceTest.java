package com.lovius.bento.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lovius.bento.dao.OrderRepository;
import com.lovius.bento.exception.ApiException;
import com.lovius.bento.model.EmployeeOrderReportRow;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmployeeOrderReportServiceTest {
    @Mock
    private OrderRepository orderRepository;

    @Mock
    private EmployeeOrderReportPdfService employeeOrderReportPdfService;

    private EmployeeOrderReportService employeeOrderReportService;

    @BeforeEach
    void setUp() {
        employeeOrderReportService = new EmployeeOrderReportService(orderRepository, employeeOrderReportPdfService);
    }

    @Test
    void getReportUsesDefaultDateSort() {
        when(orderRepository.findEmployeeOrderReportRows(
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31),
                "date"))
                .thenReturn(List.of(new EmployeeOrderReportRow(
                        LocalDate.of(2026, 3, 10),
                        "資訊部",
                        "王小明",
                        "雞腿便當",
                        "好好便當")));

        var results = employeeOrderReportService.getReport(
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31),
                null);

        assertEquals(1, results.size());
        assertEquals("資訊部", results.getFirst().departmentName());
    }

    @Test
    void getReportRejectsInvalidDateRange() {
        ApiException exception = assertThrows(ApiException.class, () ->
                employeeOrderReportService.getReport(
                        LocalDate.of(2026, 3, 31),
                        LocalDate.of(2026, 3, 1),
                        "date"));

        assertEquals("查詢起日不可晚於迄日", exception.getMessage());
    }

    @Test
    void downloadPdfPassesRowsToPdfService() {
        when(orderRepository.findEmployeeOrderReportRows(
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31),
                "supplier"))
                .thenReturn(List.of(new EmployeeOrderReportRow(
                        LocalDate.of(2026, 3, 10),
                        "資訊部",
                        "王小明",
                        "雞腿便當",
                        "好好便當")));
        when(employeeOrderReportPdfService.generatePdf(
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31),
                "supplier",
                List.of(new EmployeeOrderReportRow(
                        LocalDate.of(2026, 3, 10),
                        "資訊部",
                        "王小明",
                        "雞腿便當",
                        "好好便當"))))
                .thenReturn("%PDF".getBytes());

        byte[] bytes = employeeOrderReportService.downloadPdf(
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31),
                "supplier");

        assertEquals("%PDF", new String(bytes));
        verify(employeeOrderReportPdfService).generatePdf(
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31),
                "supplier",
                List.of(new EmployeeOrderReportRow(
                        LocalDate.of(2026, 3, 10),
                        "資訊部",
                        "王小明",
                        "雞腿便當",
                        "好好便當")));
    }
}
