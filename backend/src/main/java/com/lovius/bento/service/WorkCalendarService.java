package com.lovius.bento.service;

import com.lovius.bento.dao.WorkCalendarRepository;
import com.lovius.bento.dto.WorkCalendarDayDto;
import com.lovius.bento.exception.ApiException;
import com.lovius.bento.exception.CsvImportException;
import com.lovius.bento.model.WorkCalendar;
import java.io.IOException;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

@Service
public class WorkCalendarService {
    private static final Logger logger = LoggerFactory.getLogger(WorkCalendarService.class);
    private static final DateTimeFormatter IMPORT_DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;

    private final WorkCalendarRepository workCalendarRepository;
    private final TransactionTemplate transactionTemplate;

    public WorkCalendarService(
            WorkCalendarRepository workCalendarRepository,
            TransactionTemplate transactionTemplate) {
        this.workCalendarRepository = workCalendarRepository;
        this.transactionTemplate = transactionTemplate;
    }

    public List<WorkCalendarDayDto> getCalendar(int year, int month) {
        YearMonth yearMonth = validateYearMonth(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();
        Map<LocalDate, Boolean> storedByDate = new LinkedHashMap<>();
        for (WorkCalendar calendar : workCalendarRepository.findByDateRange(startDate, endDate)) {
            storedByDate.put(calendar.getDate(), calendar.isWorkday());
        }

        List<WorkCalendarDayDto> results = new ArrayList<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            boolean isWorkday = storedByDate.getOrDefault(date, defaultWorkday(date));
            results.add(new WorkCalendarDayDto(date, isWorkday));
        }
        return results;
    }

    public void updateCalendar(List<WorkCalendarDayDto> days) {
        if (days == null || days.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "行事曆資料不可為空");
        }
        YearMonth expectedYearMonth = null;
        List<WorkCalendar> calendars = new ArrayList<>();
        for (WorkCalendarDayDto day : days) {
            if (day.date() == null || day.isWorkday() == null) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "行事曆資料格式不完整");
            }
            YearMonth currentYearMonth = YearMonth.from(day.date());
            if (expectedYearMonth == null) {
                expectedYearMonth = currentYearMonth;
            } else if (!expectedYearMonth.equals(currentYearMonth)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "更新資料必須屬於同一月份");
            }
            calendars.add(new WorkCalendar(day.date(), day.isWorkday()));
        }
        saveAll(calendars, "更新行事曆失敗");
    }

    public void generateCalendar(int year) {
        if (year < 2000 || year > 2100) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "年份格式不正確");
        }
        List<WorkCalendar> calendars = new ArrayList<>();
        LocalDate startDate = LocalDate.of(year, 1, 1);
        LocalDate endDate = LocalDate.of(year, 12, 31);
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            calendars.add(new WorkCalendar(date, defaultWorkday(date)));
        }
        transactionTemplate.executeWithoutResult(status -> {
            try {
                workCalendarRepository.deleteByYear(year);
                workCalendarRepository.saveAll(calendars);
            } catch (DataAccessException exception) {
                logger.error("Failed to generate work calendar for year={}", year, exception);
                throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "預產行事曆失敗");
            }
        });
    }

    public List<WorkCalendarDayDto> importCalendar(MultipartFile file, boolean confirm) {
        List<WorkCalendar> parsedCalendars = parseImportFile(file);
        List<WorkCalendarDayDto> preview = parsedCalendars.stream()
                .map(calendar -> new WorkCalendarDayDto(calendar.getDate(), calendar.isWorkday()))
                .toList();
        if (!confirm) {
            return preview;
        }
        saveAll(parsedCalendars, "CSV 匯入行事曆失敗");
        return preview;
    }

    private void saveAll(List<WorkCalendar> calendars, String errorMessage) {
        transactionTemplate.executeWithoutResult(status -> {
            try {
                workCalendarRepository.saveAll(calendars);
            } catch (DataAccessException exception) {
                logger.error("{}: size={}", errorMessage, calendars.size(), exception);
                throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, errorMessage);
            }
        });
    }

    private List<WorkCalendar> parseImportFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new CsvImportException(HttpStatus.BAD_REQUEST, "請上傳 CSV 檔案", null, "file 為必填");
        }

        String csvContent;
        try {
            csvContent = decodeCsv(file);
        } catch (IOException exception) {
            logger.error("Failed to read work calendar CSV file: {}", file.getOriginalFilename(), exception);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "讀取 CSV 檔案失敗");
        }

        try (CSVParser parser = CSVFormat.DEFAULT
                .builder()
                .setTrim(true)
                .build()
                .parse(new StringReader(csvContent))) {
            Map<LocalDate, Boolean> rows = new LinkedHashMap<>();
            for (CSVRecord record : parser) {
                int lineNumber = (int) record.getRecordNumber();
                if (record.size() == 1 && record.get(0).isBlank()) {
                    continue;
                }
                if (record.size() != 2) {
                    throw new CsvImportException(
                            HttpStatus.UNPROCESSABLE_ENTITY,
                            "CSV 匯入失敗",
                            lineNumber,
                            "每行格式必須為 yyyymmdd,y/n");
                }

                LocalDate date = parseImportDate(record.get(0), lineNumber);
                boolean isWorkday = parseWorkdayFlag(record.get(1), lineNumber);
                rows.put(date, isWorkday);
            }

            if (rows.isEmpty()) {
                throw new CsvImportException(HttpStatus.BAD_REQUEST, "CSV 內容不可為空", null, "檔案沒有可匯入資料");
            }

            return rows.entrySet().stream()
                    .map(entry -> new WorkCalendar(entry.getKey(), entry.getValue()))
                    .toList();
        } catch (CsvImportException exception) {
            logger.warn(
                    "Work calendar CSV import validation failed: file={}, line={}, reason={}",
                    file.getOriginalFilename(),
                    exception.getFailedAtLine(),
                    exception.getReason());
            throw exception;
        } catch (IOException exception) {
            logger.error("Failed to parse work calendar CSV file: {}", file.getOriginalFilename(), exception);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "解析 CSV 檔案失敗");
        }
    }

    private String decodeCsv(MultipartFile file) throws IOException {
        byte[] bytes = file.getBytes();
        CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        try {
            return decoder.decode(ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException exception) {
            throw new CsvImportException(HttpStatus.BAD_REQUEST, "CSV 必須為 UTF-8 編碼", null, "檔案編碼錯誤");
        }
    }

    private LocalDate parseImportDate(String rawValue, int lineNumber) {
        try {
            return LocalDate.parse(rawValue.trim(), IMPORT_DATE_FORMATTER);
        } catch (DateTimeException exception) {
            throw new CsvImportException(HttpStatus.UNPROCESSABLE_ENTITY, "CSV 匯入失敗", lineNumber, "日期格式必須為 yyyymmdd");
        }
    }

    private boolean parseWorkdayFlag(String rawValue, int lineNumber) {
        String normalizedValue = rawValue.trim().toLowerCase(Locale.ROOT);
        if ("y".equals(normalizedValue)) {
            return true;
        }
        if ("n".equals(normalizedValue)) {
            return false;
        }
        throw new CsvImportException(HttpStatus.UNPROCESSABLE_ENTITY, "CSV 匯入失敗", lineNumber, "上班日旗標必須為 y 或 n");
    }

    private YearMonth validateYearMonth(int year, int month) {
        if (year < 2000 || year > 2100) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "年份格式不正確");
        }
        if (month < 1 || month > 12) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "月份格式不正確");
        }
        return YearMonth.of(year, month);
    }

    private boolean defaultWorkday(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY;
    }
}
