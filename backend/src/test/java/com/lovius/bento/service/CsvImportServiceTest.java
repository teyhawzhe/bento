package com.lovius.bento.service;

import com.lovius.bento.dao.DepartmentRepository;
import com.lovius.bento.dao.EmployeeRepository;
import com.lovius.bento.dao.MenuRepository;
import com.lovius.bento.dao.SupplierRepository;
import com.lovius.bento.exception.CsvImportException;
import com.lovius.bento.model.Department;
import com.lovius.bento.model.Employee;
import com.lovius.bento.model.Menu;
import com.lovius.bento.model.Supplier;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
class CsvImportServiceTest {
    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private SupplierRepository supplierRepository;

    @Mock
    private MenuRepository menuRepository;

    @Mock
    private PasswordPolicyService passwordPolicyService;

    @Mock
    private EmailService emailService;

    @Mock
    private PlatformTransactionManager transactionManager;

    private CsvImportService csvImportService;

    @BeforeEach
    void setUp() {
        Mockito.lenient().when(transactionManager.getTransaction(ArgumentMatchers.any(TransactionDefinition.class)))
                .thenReturn(Mockito.mock(TransactionStatus.class));
        csvImportService = new CsvImportService(
                employeeRepository,
                departmentRepository,
                supplierRepository,
                menuRepository,
                passwordPolicyService,
                emailService,
                new TransactionTemplate(transactionManager));
    }

    @Test
    void importEmployeesUsesDepartmentIdHeaderAndReturnsImportedRows() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "employees.csv",
                "text/csv",
                ("username,name,email,department_id\n"
                        + "alice,Alice,alice@company.local,2\n").getBytes(StandardCharsets.UTF_8));
        Department department = new Department(2L, "Operations", true, Instant.now(), Instant.now());
        Mockito.when(employeeRepository.existsByUsername("alice")).thenReturn(false);
        Mockito.when(employeeRepository.existsByEmail("alice@company.local")).thenReturn(false);
        Mockito.when(departmentRepository.findById(2L)).thenReturn(Optional.of(department));
        Mockito.when(passwordPolicyService.generateTemporaryPassword()).thenReturn("WelcomeA1");
        Mockito.when(passwordPolicyService.hash("WelcomeA1")).thenReturn("hashed");
        Mockito.when(employeeRepository.save(ArgumentMatchers.any(Employee.class))).thenAnswer(invocation -> {
            Employee employee = invocation.getArgument(0);
            employee.setId(5L);
            return employee;
        });

        var results = csvImportService.importEmployees(file);

        Assertions.assertEquals(1, results.size());
        Assertions.assertEquals(2L, results.getFirst().departmentId());
        Assertions.assertFalse(results.getFirst().isAdmin());
        Mockito.verify(emailService).sendPasswordEmail("alice@company.local", "新建員工帳號通知", "WelcomeA1");
    }

    @Test
    void importDepartmentsRejectsDuplicateNamesWithinBatch() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "departments.csv",
                "text/csv",
                ("name\n"
                        + "IT\n"
                        + "IT\n").getBytes(StandardCharsets.UTF_8));
        Mockito.when(departmentRepository.findByName("IT")).thenReturn(Optional.empty());

        CsvImportException exception = Assertions.assertThrows(
                CsvImportException.class,
                () -> csvImportService.importDepartments(file));

        Assertions.assertEquals(422, exception.getStatus().value());
        Assertions.assertEquals(3, exception.getFailedAtLine());
        Assertions.assertEquals("name 與同批次資料重複", exception.getReason());
    }

    @Test
    void importSuppliersRejectsExistingBusinessRegistrationNumber() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "suppliers.csv",
                "text/csv",
                ("name,email,phone,contact_person,business_registration_no\n"
                        + "Vendor,orders@vendor.local,02-1234-5678,Alice,12345678\n").getBytes(StandardCharsets.UTF_8));
        Mockito.when(supplierRepository.findByBusinessRegistrationNo("12345678"))
                .thenReturn(Optional.of(new Supplier(9L, "Vendor", "orders@vendor.local", "02", "Alice", "12345678", true, Instant.now())));

        CsvImportException exception = Assertions.assertThrows(
                CsvImportException.class,
                () -> csvImportService.importSuppliers(file));

        Assertions.assertEquals("business_registration_no 已存在", exception.getReason());
    }

    @Test
    void importMenusRejectsInvalidDateRange() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "menus.csv",
                "text/csv",
                ("supplier_id,name,category,description,price,valid_from,valid_to\n"
                        + "5,Chicken,肉類,desc,120,2026-03-30,2026-03-20\n").getBytes(StandardCharsets.UTF_8));
        Mockito.when(supplierRepository.findById(5L))
                .thenReturn(Optional.of(new Supplier(5L, "Vendor", "orders@vendor.local", "02", "Alice", "12345678", true, Instant.now())));
        Mockito.when(menuRepository.findBySupplierIdAndName(5L, "Chicken")).thenReturn(Optional.empty());

        CsvImportException exception = Assertions.assertThrows(
                CsvImportException.class,
                () -> csvImportService.importMenus(1L, file));

        Assertions.assertEquals("valid_from 不可晚於 valid_to", exception.getReason());
    }

    @Test
    void getTemplateReturnsExpectedHeaderLine() {
        Assertions.assertEquals(
                "name,email,phone,contact_person,business_registration_no\n",
                csvImportService.getTemplate("suppliers"));
    }
}
