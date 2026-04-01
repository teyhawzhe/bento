package com.lovius.bento.service;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lovius.bento.dao.WorkCalendarRepository;
import com.lovius.bento.dto.WorkCalendarDayDto;
import com.lovius.bento.exception.ApiException;
import com.lovius.bento.exception.CsvImportException;
import com.lovius.bento.model.WorkCalendar;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(MockitoExtension.class)
class WorkCalendarServiceTest {
    @Mock
    private WorkCalendarRepository workCalendarRepository;

    @Mock
    private PlatformTransactionManager transactionManager;

    private WorkCalendarService workCalendarService;

    @BeforeEach
    void setUp() {
        Mockito.lenient().when(transactionManager.getTransaction(ArgumentMatchers.any(TransactionDefinition.class)))
                .thenReturn(Mockito.mock(TransactionStatus.class));
        workCalendarService = new WorkCalendarService(
                workCalendarRepository,
                new TransactionTemplate(transactionManager));
    }

    @Test
    void getCalendarReturnsStoredAndDefaultDays() {
        when(workCalendarRepository.findByDateRange(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30)))
                .thenReturn(List.of(
                        new WorkCalendar(LocalDate.of(2026, 4, 1), false),
                        new WorkCalendar(LocalDate.of(2026, 4, 2), true)));

        List<WorkCalendarDayDto> result = workCalendarService.getCalendar(2026, 4);

        Assertions.assertEquals(30, result.size());
        Assertions.assertFalse(result.getFirst().isWorkday());
        Assertions.assertTrue(result.get(1).isWorkday());
        Assertions.assertFalse(result.stream()
                .filter(day -> day.date().equals(LocalDate.of(2026, 4, 4)))
                .findFirst()
                .orElseThrow()
                .isWorkday());
    }

    @Test
    void updateCalendarRejectsCrossMonthPayload() {
        ApiException exception = Assertions.assertThrows(
                ApiException.class,
                () -> workCalendarService.updateCalendar(List.of(
                        new WorkCalendarDayDto(LocalDate.of(2026, 4, 30), true),
                        new WorkCalendarDayDto(LocalDate.of(2026, 5, 1), false))));

        Assertions.assertEquals("更新資料必須屬於同一月份", exception.getMessage());
    }

    @Test
    void generateCalendarReplacesWholeYear() {
        workCalendarService.generateCalendar(2026);

        verify(workCalendarRepository).deleteByYear(2026);
        ArgumentCaptor<List<WorkCalendar>> captor = ArgumentCaptor.forClass(List.class);
        verify(workCalendarRepository).saveAll(captor.capture());
        Assertions.assertEquals(365, captor.getValue().size());
        Assertions.assertFalse(captor.getValue().get(2).isWorkday());
    }

    @Test
    void importCalendarPreviewParsesCsvWithoutSaving() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "calendar.csv",
                "text/csv",
                ("20260401,y\n"
                        + "20260402,n\n").getBytes(StandardCharsets.UTF_8));

        List<WorkCalendarDayDto> result = workCalendarService.importCalendar(file, false);

        Assertions.assertEquals(2, result.size());
        Assertions.assertTrue(result.getFirst().isWorkday());
        Assertions.assertFalse(result.get(1).isWorkday());
        verify(workCalendarRepository, Mockito.never()).saveAll(anyList());
    }

    @Test
    void importCalendarConfirmSavesParsedRows() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "calendar.csv",
                "text/csv",
                "20260401,y\n".getBytes(StandardCharsets.UTF_8));

        workCalendarService.importCalendar(file, true);

        ArgumentCaptor<List<WorkCalendar>> captor = ArgumentCaptor.forClass(List.class);
        verify(workCalendarRepository).saveAll(captor.capture());
        Assertions.assertEquals(1, captor.getValue().size());
        Assertions.assertEquals(LocalDate.of(2026, 4, 1), captor.getValue().getFirst().getDate());
        Assertions.assertTrue(captor.getValue().getFirst().isWorkday());
    }

    @Test
    void importCalendarRejectsInvalidFlag() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "calendar.csv",
                "text/csv",
                "20260401,x\n".getBytes(StandardCharsets.UTF_8));

        CsvImportException exception = Assertions.assertThrows(
                CsvImportException.class,
                () -> workCalendarService.importCalendar(file, false));

        Assertions.assertEquals(1, exception.getFailedAtLine());
        Assertions.assertEquals("上班日旗標必須為 y 或 n", exception.getReason());
    }
}
