package com.lovius.bento.service;

import com.lovius.bento.dao.DepartmentRepository;
import com.lovius.bento.dao.EmployeeRepository;
import com.lovius.bento.dao.MenuRepository;
import com.lovius.bento.dao.SupplierRepository;
import com.lovius.bento.dto.ImportDepartmentResult;
import com.lovius.bento.dto.ImportEmployeeResult;
import com.lovius.bento.dto.ImportMenuResult;
import com.lovius.bento.dto.ImportSupplierResult;
import com.lovius.bento.exception.ApiException;
import com.lovius.bento.exception.CsvImportException;
import com.lovius.bento.model.Department;
import com.lovius.bento.model.Employee;
import com.lovius.bento.model.Menu;
import com.lovius.bento.model.Supplier;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.charset.CharsetDecoder;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CsvImportService {
    private static final int BATCH_SIZE = 5000;
    private static final List<String> EMPLOYEE_HEADERS = List.of("username", "name", "email", "department_id");
    private static final List<String> DEPARTMENT_HEADERS = List.of("name");
    private static final List<String> SUPPLIER_HEADERS = List.of(
            "name",
            "email",
            "phone",
            "contact_person",
            "business_registration_no");
    private static final List<String> MENU_HEADERS = List.of(
            "supplier_id",
            "name",
            "category",
            "description",
            "price",
            "valid_from",
            "valid_to");

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final SupplierRepository supplierRepository;
    private final MenuRepository menuRepository;
    private final PasswordPolicyService passwordPolicyService;
    private final EmailService emailService;
    private final TransactionTemplate transactionTemplate;

    public CsvImportService(
            EmployeeRepository employeeRepository,
            DepartmentRepository departmentRepository,
            SupplierRepository supplierRepository,
            MenuRepository menuRepository,
            PasswordPolicyService passwordPolicyService,
            EmailService emailService,
            TransactionTemplate transactionTemplate) {
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
        this.supplierRepository = supplierRepository;
        this.menuRepository = menuRepository;
        this.passwordPolicyService = passwordPolicyService;
        this.emailService = emailService;
        this.transactionTemplate = transactionTemplate;
    }

    public String getTemplate(String type) {
        return String.join(",", headersFor(type)) + "\n";
    }

    public List<ImportEmployeeResult> importEmployees(MultipartFile file) {
        List<EmployeeCsvRow> rows = parseRows(file, EMPLOYEE_HEADERS).stream()
                .map(this::toEmployeeRow)
                .toList();
        List<ImportEmployeeResult> imported = new ArrayList<>();
        for (List<EmployeeCsvRow> batch : batches(rows)) {
            imported.addAll(importEmployeeBatch(batch));
        }
        return imported;
    }

    public List<ImportDepartmentResult> importDepartments(MultipartFile file) {
        List<DepartmentCsvRow> rows = parseRows(file, DEPARTMENT_HEADERS).stream()
                .map(this::toDepartmentRow)
                .toList();
        List<ImportDepartmentResult> imported = new ArrayList<>();
        for (List<DepartmentCsvRow> batch : batches(rows)) {
            imported.addAll(importDepartmentBatch(batch));
        }
        return imported;
    }

    public List<ImportSupplierResult> importSuppliers(MultipartFile file) {
        List<SupplierCsvRow> rows = parseRows(file, SUPPLIER_HEADERS).stream()
                .map(this::toSupplierRow)
                .toList();
        List<ImportSupplierResult> imported = new ArrayList<>();
        for (List<SupplierCsvRow> batch : batches(rows)) {
            imported.addAll(importSupplierBatch(batch));
        }
        return imported;
    }

    public List<ImportMenuResult> importMenus(Long createdBy, MultipartFile file) {
        List<MenuCsvRow> rows = parseRows(file, MENU_HEADERS).stream()
                .map(this::toMenuRow)
                .toList();
        List<ImportMenuResult> imported = new ArrayList<>();
        for (List<MenuCsvRow> batch : batches(rows)) {
            imported.addAll(importMenuBatch(createdBy, batch));
        }
        return imported;
    }

    private List<ImportEmployeeResult> importEmployeeBatch(List<EmployeeCsvRow> batch) {
        Set<String> usernames = new LinkedHashSet<>();
        Set<String> emails = new LinkedHashSet<>();
        List<EmployeeImportCandidate> candidates = new ArrayList<>();
        for (EmployeeCsvRow row : batch) {
            String usernameKey = normalizeKey(row.username());
            if (!usernames.add(usernameKey)) {
                throw batchFailure(row.line(), "username 與同批次資料重複");
            }
            String emailKey = normalizeKey(row.email());
            if (!emails.add(emailKey)) {
                throw batchFailure(row.line(), "email 與同批次資料重複");
            }
            if (employeeRepository.existsByUsername(row.username())) {
                throw batchFailure(row.line(), "username 已存在");
            }
            if (employeeRepository.existsByEmail(row.email())) {
                throw batchFailure(row.line(), "Email 已存在");
            }
            Department department = departmentRepository.findById(row.departmentId())
                    .orElseThrow(() -> batchFailure(row.line(), "department_id 不存在"));
            if (!department.isActive()) {
                throw batchFailure(row.line(), "department_id 對應部門已停用");
            }
            String generatedPassword = passwordPolicyService.generateTemporaryPassword();
            Instant now = Instant.now();
            candidates.add(new EmployeeImportCandidate(
                    row.line(),
                    row.email(),
                    generatedPassword,
                    new Employee(
                            null,
                            department.getId(),
                            department.getName(),
                            row.username(),
                            passwordPolicyService.hash(generatedPassword),
                            row.name(),
                            row.email(),
                            false,
                            true,
                            now,
                            now)));
        }

        List<ImportEmployeeResult> imported = transactionTemplate.execute(status -> {
            try {
                List<ImportEmployeeResult> results = new ArrayList<>();
                for (EmployeeImportCandidate candidate : candidates) {
                    Employee saved = employeeRepository.save(candidate.employee());
                    emailService.sendPasswordEmail(candidate.email(), "新建員工帳號通知", candidate.generatedPassword());
                    results.add(new ImportEmployeeResult(
                            saved.getId(),
                            saved.getUsername(),
                            saved.getName(),
                            saved.getEmail(),
                            saved.getDepartmentId(),
                            saved.isAdmin(),
                            saved.isActive()));
                }
                return results;
            } catch (DataAccessException exception) {
                throw batchFailure(candidates.getFirst().line(), "資料庫寫入失敗");
            }
        });
        return imported == null ? List.of() : imported;
    }

    private List<ImportDepartmentResult> importDepartmentBatch(List<DepartmentCsvRow> batch) {
        Set<String> names = new LinkedHashSet<>();
        List<Department> departments = new ArrayList<>();
        for (DepartmentCsvRow row : batch) {
            String key = normalizeKey(row.name());
            if (!names.add(key)) {
                throw batchFailure(row.line(), "name 與同批次資料重複");
            }
            if (departmentRepository.findByName(row.name()).isPresent()) {
                throw batchFailure(row.line(), "部門名稱已存在");
            }
            Instant now = Instant.now();
            departments.add(new Department(null, row.name(), true, now, now));
        }
        List<ImportDepartmentResult> imported = transactionTemplate.execute(status -> {
            try {
                List<ImportDepartmentResult> results = new ArrayList<>();
                for (Department department : departments) {
                    Department saved = departmentRepository.save(department);
                    results.add(new ImportDepartmentResult(saved.getId(), saved.getName()));
                }
                return results;
            } catch (DataAccessException exception) {
                throw batchFailure(batch.getFirst().line(), "資料庫寫入失敗");
            }
        });
        return imported == null ? List.of() : imported;
    }

    private List<ImportSupplierResult> importSupplierBatch(List<SupplierCsvRow> batch) {
        Set<String> registrationNos = new LinkedHashSet<>();
        List<Supplier> suppliers = new ArrayList<>();
        for (SupplierCsvRow row : batch) {
            String key = normalizeKey(row.businessRegistrationNo());
            if (!registrationNos.add(key)) {
                throw batchFailure(row.line(), "business_registration_no 與同批次資料重複");
            }
            if (supplierRepository.findByBusinessRegistrationNo(row.businessRegistrationNo()).isPresent()) {
                throw batchFailure(row.line(), "business_registration_no 已存在");
            }
            suppliers.add(new Supplier(
                    null,
                    row.name(),
                    row.email(),
                    row.phone(),
                    row.contactPerson(),
                    row.businessRegistrationNo(),
                    true,
                    Instant.now()));
        }
        List<ImportSupplierResult> imported = transactionTemplate.execute(status -> {
            try {
                List<ImportSupplierResult> results = new ArrayList<>();
                for (Supplier supplier : suppliers) {
                    Supplier saved = supplierRepository.save(supplier);
                    results.add(new ImportSupplierResult(
                            saved.getId(),
                            saved.getName(),
                            saved.getEmail(),
                            saved.getPhone(),
                            saved.getContactPerson(),
                            saved.getBusinessRegistrationNo(),
                            saved.isActive()));
                }
                return results;
            } catch (DataAccessException exception) {
                throw batchFailure(batch.getFirst().line(), "資料庫寫入失敗");
            }
        });
        return imported == null ? List.of() : imported;
    }

    private List<ImportMenuResult> importMenuBatch(Long createdBy, List<MenuCsvRow> batch) {
        Set<String> uniqueMenuKeys = new LinkedHashSet<>();
        List<Menu> menus = new ArrayList<>();
        for (MenuCsvRow row : batch) {
            String key = row.supplierId() + ":" + normalizeKey(row.name());
            if (!uniqueMenuKeys.add(key)) {
                throw batchFailure(row.line(), "supplier_id + name 與同批次資料重複");
            }
            Supplier supplier = supplierRepository.findById(row.supplierId())
                    .orElseThrow(() -> batchFailure(row.line(), "supplier_id 不存在"));
            if (!supplier.isActive()) {
                throw batchFailure(row.line(), "supplier_id 對應供應商已停用");
            }
            if (menuRepository.findBySupplierIdAndName(row.supplierId(), row.name()).isPresent()) {
                throw batchFailure(row.line(), "supplier_id + name 已存在");
            }
            if (row.validFrom().isAfter(row.validTo())) {
                throw batchFailure(row.line(), "valid_from 不可晚於 valid_to");
            }
            Instant now = Instant.now();
            menus.add(new Menu(
                    null,
                    row.supplierId(),
                    row.name(),
                    row.category(),
                    row.description(),
                    row.price(),
                    row.validFrom(),
                    row.validTo(),
                    createdBy,
                    now,
                    now));
        }
        List<ImportMenuResult> imported = transactionTemplate.execute(status -> {
            try {
                List<ImportMenuResult> results = new ArrayList<>();
                for (Menu menu : menus) {
                    Menu saved = menuRepository.save(menu);
                    results.add(new ImportMenuResult(
                            saved.getId(),
                            saved.getSupplierId(),
                            saved.getName(),
                            saved.getCategory(),
                            saved.getDescription(),
                            saved.getPrice(),
                            saved.getValidFrom(),
                            saved.getValidTo()));
                }
                return results;
            } catch (DataAccessException exception) {
                throw batchFailure(batch.getFirst().line(), "資料庫寫入失敗");
            }
        });
        return imported == null ? List.of() : imported;
    }

    private List<ParsedCsvRow> parseRows(MultipartFile file, List<String> expectedHeaders) {
        if (file.isEmpty()) {
            throw new CsvImportException(HttpStatus.BAD_REQUEST, "請上傳 CSV 檔案", null, "檔案不可為空");
        }
        String content = decodeUtf8(file);
        try (CSVParser parser = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreHeaderCase(false)
                .setTrim(true)
                .build()
                .parse(new StringReader(content))) {
            List<String> actualHeaders = parser.getHeaderNames();
            if (!expectedHeaders.equals(actualHeaders)) {
                throw new CsvImportException(
                        HttpStatus.BAD_REQUEST,
                        "CSV 表頭格式不正確",
                        null,
                        "表頭必須為 " + String.join(",", expectedHeaders));
            }
            List<ParsedCsvRow> rows = new ArrayList<>();
            for (CSVRecord record : parser) {
                rows.add(new ParsedCsvRow((int) record.getRecordNumber() + 1, record));
            }
            return rows;
        } catch (IOException exception) {
            throw new CsvImportException(HttpStatus.BAD_REQUEST, "CSV 讀取失敗", null, "無法解析 CSV 內容");
        }
    }

    private String decodeUtf8(MultipartFile file) {
        try {
            CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT);
            return decoder.decode(ByteBuffer.wrap(file.getBytes())).toString();
        } catch (CharacterCodingException exception) {
            throw new CsvImportException(HttpStatus.BAD_REQUEST, "CSV 必須為 UTF-8 編碼", null, "檔案編碼不正確");
        } catch (IOException exception) {
            throw new CsvImportException(HttpStatus.BAD_REQUEST, "CSV 讀取失敗", null, "無法讀取上傳檔案");
        }
    }

    private List<String> headersFor(String type) {
        return switch (type.toLowerCase(Locale.ROOT)) {
            case "employees" -> EMPLOYEE_HEADERS;
            case "departments" -> DEPARTMENT_HEADERS;
            case "suppliers" -> SUPPLIER_HEADERS;
            case "menus" -> MENU_HEADERS;
            default -> throw new ApiException(HttpStatus.BAD_REQUEST, "type 僅支援 employees、departments、suppliers、menus");
        };
    }

    private EmployeeCsvRow toEmployeeRow(ParsedCsvRow row) {
        return new EmployeeCsvRow(
                row.line(),
                required(row.record(), "username", row.line()),
                required(row.record(), "name", row.line()),
                required(row.record(), "email", row.line()),
                parseLong(required(row.record(), "department_id", row.line()), "department_id", row.line()));
    }

    private DepartmentCsvRow toDepartmentRow(ParsedCsvRow row) {
        return new DepartmentCsvRow(row.line(), required(row.record(), "name", row.line()));
    }

    private SupplierCsvRow toSupplierRow(ParsedCsvRow row) {
        return new SupplierCsvRow(
                row.line(),
                required(row.record(), "name", row.line()),
                required(row.record(), "email", row.line()),
                required(row.record(), "phone", row.line()),
                required(row.record(), "contact_person", row.line()),
                required(row.record(), "business_registration_no", row.line()));
    }

    private MenuCsvRow toMenuRow(ParsedCsvRow row) {
        return new MenuCsvRow(
                row.line(),
                parseLong(required(row.record(), "supplier_id", row.line()), "supplier_id", row.line()),
                required(row.record(), "name", row.line()),
                required(row.record(), "category", row.line()),
                required(row.record(), "description", row.line()),
                parseDecimal(required(row.record(), "price", row.line()), "price", row.line()),
                parseDate(required(row.record(), "valid_from", row.line()), "valid_from", row.line()),
                parseDate(required(row.record(), "valid_to", row.line()), "valid_to", row.line()));
    }

    private String required(CSVRecord record, String key, int line) {
        String value = record.get(key);
        if (value == null || value.isBlank()) {
            throw batchFailure(line, key + " 為必填");
        }
        return value.trim();
    }

    private Long parseLong(String value, String key, int line) {
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException exception) {
            throw batchFailure(line, key + " 格式錯誤");
        }
    }

    private BigDecimal parseDecimal(String value, String key, int line) {
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException exception) {
            throw batchFailure(line, key + " 格式錯誤");
        }
    }

    private LocalDate parseDate(String value, String key, int line) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException exception) {
            throw batchFailure(line, key + " 格式錯誤");
        }
    }

    private String normalizeKey(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private <T> List<List<T>> batches(List<T> rows) {
        List<List<T>> batches = new ArrayList<>();
        for (int start = 0; start < rows.size(); start += BATCH_SIZE) {
            int end = Math.min(start + BATCH_SIZE, rows.size());
            batches.add(rows.subList(start, end));
        }
        return batches;
    }

    private CsvImportException batchFailure(int line, String reason) {
        return new CsvImportException(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "第 " + line + " 行驗證失敗，已 Rollback，請手動刪除已處理列後重新上傳",
                line,
                reason);
    }

    private record ParsedCsvRow(int line, CSVRecord record) {
    }

    private record EmployeeCsvRow(int line, String username, String name, String email, Long departmentId) {
    }

    private record EmployeeImportCandidate(int line, String email, String generatedPassword, Employee employee) {
    }

    private record DepartmentCsvRow(int line, String name) {
    }

    private record SupplierCsvRow(
            int line,
            String name,
            String email,
            String phone,
            String contactPerson,
            String businessRegistrationNo) {
    }

    private record MenuCsvRow(
            int line,
            Long supplierId,
            String name,
            String category,
            String description,
            BigDecimal price,
            LocalDate validFrom,
            LocalDate validTo) {
    }
}
