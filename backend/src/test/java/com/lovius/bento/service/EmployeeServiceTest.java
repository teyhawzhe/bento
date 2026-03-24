package com.lovius.bento.service;

import com.lovius.bento.dao.EmployeeRepository;
import com.lovius.bento.dto.CreateEmployeeRequest;
import com.lovius.bento.dto.ResetEmployeePasswordRequest;
import com.lovius.bento.dto.UpdateEmployeeRequest;
import com.lovius.bento.exception.ApiException;
import com.lovius.bento.model.Department;
import com.lovius.bento.model.Employee;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
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

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {
    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private DepartmentService departmentService;

    @Mock
    private PasswordPolicyService passwordPolicyService;

    @Mock
    private EmailService emailService;

    @Mock
    private RefreshTokenService refreshTokenService;

    private EmployeeService employeeService;

    @BeforeEach
    void setUp() {
        employeeService = new EmployeeService(
                employeeRepository,
                departmentService,
                passwordPolicyService,
                emailService,
                refreshTokenService);
    }

    @Test
    void createEmployeeIncludesDepartmentInResponse() {
        Mockito.when(employeeRepository.existsByUsername("alice")).thenReturn(false);
        Mockito.when(employeeRepository.existsByEmail("alice@company.local")).thenReturn(false);
        Mockito.when(departmentService.getActiveDepartment(2L))
                .thenReturn(new Department(2L, "Operations", true, Instant.now(), Instant.now()));
        Mockito.when(passwordPolicyService.generateTemporaryPassword()).thenReturn("WelcomeA1");
        Mockito.when(passwordPolicyService.hash("WelcomeA1")).thenReturn("hashed-password");

        employeeService.createEmployee(
                new CreateEmployeeRequest("alice", "Alice Chen", "alice@company.local", 2L));

        Mockito.verify(employeeRepository).save(ArgumentMatchers.any(Employee.class));
        Mockito.verify(emailService)
                .sendPasswordEmail("alice@company.local", "新建員工帳號通知", "WelcomeA1");
    }

    @Test
    void importEmployeesRequiresDepartmentColumnValues() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "employees.csv",
                "text/csv",
                "username,name,email,department\nalice,Alice,alice@company.local,\n"
                        .getBytes(StandardCharsets.UTF_8));

        var response = employeeService.importEmployees(file);

        Assertions.assertEquals(0, response.successCount());
        Assertions.assertEquals(1, response.failureCount());
        Assertions.assertEquals("department 為必填", response.errors().getFirst().reason());
    }

    @Test
    void resetPasswordRevokesRefreshTokensAndPersistsHash() {
        Employee employee = new Employee(
                8L,
                3L,
                "Finance",
                "cindy",
                "old",
                "Cindy",
                "cindy@company.local",
                false,
                true,
                Instant.now(),
                Instant.now());
        Mockito.when(employeeRepository.findById(8L)).thenReturn(Optional.of(employee));
        Mockito.when(passwordPolicyService.hash("NewPassA1")).thenReturn("new-hash");

        employeeService.resetPassword(8L, new ResetEmployeePasswordRequest("NewPassA1"));

        Assertions.assertEquals("new-hash", employee.getPasswordHash());
        Mockito.verify(refreshTokenService).revokeAllByEmployeeId(8L);
    }

    @Test
    void createEmployeeRejectsDuplicateUsername() {
        Mockito.when(employeeRepository.existsByUsername("alice")).thenReturn(true);

        ApiException exception = Assertions.assertThrows(ApiException.class,
                () -> employeeService.createEmployee(
                        new CreateEmployeeRequest("alice", "Alice", "alice@company.local", 1L)));

        Assertions.assertEquals("username 已存在", exception.getMessage());
    }

    @Test
    void updateEmployeeAllowsKeepingExistingUsernameAndEmail() {
        Instant now = Instant.parse("2026-03-24T09:00:00Z");
        Employee employee = new Employee(
                5L,
                2L,
                "Operations",
                "alice",
                "hash",
                "Alice Chen",
                "alice@company.local",
                false,
                true,
                now,
                now);
        Department sales = new Department(4L, "Sales", true, now, now);
        Mockito.when(employeeRepository.findById(5L)).thenReturn(Optional.of(employee));
        Mockito.when(employeeRepository.findByUsername("alice")).thenReturn(Optional.of(employee));
        Mockito.when(employeeRepository.findByEmail("alice@company.local")).thenReturn(Optional.of(employee));
        Mockito.when(departmentService.getActiveDepartment(4L)).thenReturn(sales);

        var response = employeeService.updateEmployee(
                5L,
                new UpdateEmployeeRequest("alice", "Alice Wang", "alice@company.local", 4L, true));

        Assertions.assertEquals("Alice Wang", response.name());
        Assertions.assertEquals("Sales", response.department().name());
        Assertions.assertTrue(response.isAdmin());
        Mockito.verify(employeeRepository).save(employee);
    }

    @Test
    void updateEmployeeRejectsDuplicateEmailUsedByAnotherEmployee() {
        Instant now = Instant.parse("2026-03-24T09:00:00Z");
        Employee employee = new Employee(
                5L,
                2L,
                "Operations",
                "alice",
                "hash",
                "Alice Chen",
                "alice@company.local",
                false,
                true,
                now,
                now);
        Employee other = new Employee(
                6L,
                3L,
                "Finance",
                "bob",
                "hash",
                "Bob",
                "bob@company.local",
                false,
                true,
                now,
                now);
        Mockito.when(employeeRepository.findById(5L)).thenReturn(Optional.of(employee));
        Mockito.when(employeeRepository.findByUsername("alice")).thenReturn(Optional.of(employee));
        Mockito.when(employeeRepository.findByEmail("bob@company.local")).thenReturn(Optional.of(other));

        ApiException exception = Assertions.assertThrows(ApiException.class,
                () -> employeeService.updateEmployee(
                        5L,
                        new UpdateEmployeeRequest("alice", "Alice Chen", "bob@company.local", 2L, false)));

        Assertions.assertEquals("Email 已被其他員工使用", exception.getMessage());
    }

    @Test
    void updateEmployeeRequiresActiveDepartment() {
        Instant now = Instant.parse("2026-03-24T09:00:00Z");
        Employee employee = new Employee(
                5L,
                2L,
                "Operations",
                "alice",
                "hash",
                "Alice Chen",
                "alice@company.local",
                false,
                true,
                now,
                now);
        Mockito.when(employeeRepository.findById(5L)).thenReturn(Optional.of(employee));
        Mockito.when(employeeRepository.findByUsername("alice")).thenReturn(Optional.of(employee));
        Mockito.when(employeeRepository.findByEmail("alice@company.local")).thenReturn(Optional.of(employee));
        Mockito.when(departmentService.getActiveDepartment(99L))
                .thenThrow(new ApiException(org.springframework.http.HttpStatus.BAD_REQUEST, "部門不存在或已停用"));

        ApiException exception = Assertions.assertThrows(ApiException.class,
                () -> employeeService.updateEmployee(
                        5L,
                        new UpdateEmployeeRequest("alice", "Alice Chen", "alice@company.local", 99L, false)));

        Assertions.assertEquals("部門不存在或已停用", exception.getMessage());
    }
}
