package com.lovius.bento.service;

import com.lovius.bento.dao.EmployeeRepository;
import com.lovius.bento.dto.CreateEmployeeRequest;
import com.lovius.bento.dto.DepartmentSummaryResponse;
import com.lovius.bento.dto.EmployeeStatusRequest;
import com.lovius.bento.dto.EmployeeSummaryResponse;
import com.lovius.bento.dto.ImportEmployeesResponse;
import com.lovius.bento.dto.ImportedEmployeeError;
import com.lovius.bento.dto.ResetEmployeePasswordRequest;
import com.lovius.bento.dto.UpdateEmployeeRequest;
import com.lovius.bento.exception.ApiException;
import com.lovius.bento.model.Department;
import com.lovius.bento.model.Employee;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class EmployeeService {
    private final EmployeeRepository employeeRepository;
    private final DepartmentService departmentService;
    private final PasswordPolicyService passwordPolicyService;
    private final EmailService emailService;
    private final RefreshTokenService refreshTokenService;

    public EmployeeService(
            EmployeeRepository employeeRepository,
            DepartmentService departmentService,
            PasswordPolicyService passwordPolicyService,
            EmailService emailService,
            RefreshTokenService refreshTokenService) {
        this.employeeRepository = employeeRepository;
        this.departmentService = departmentService;
        this.passwordPolicyService = passwordPolicyService;
        this.emailService = emailService;
        this.refreshTokenService = refreshTokenService;
    }

    public List<EmployeeSummaryResponse> getAllEmployees() {
        return employeeRepository.findAll()
                .stream()
                .map(this::toSummary)
                .toList();
    }

    public void createEmployee(CreateEmployeeRequest request) {
        validateUnique(request.username(), request.email());
        Department department = departmentService.getActiveDepartment(request.departmentId());
        String generatedPassword = passwordPolicyService.generateTemporaryPassword();
        Instant now = Instant.now();
        Employee employee = new Employee(
                null,
                department.getId(),
                department.getName(),
                request.username().trim(),
                passwordPolicyService.hash(generatedPassword),
                request.name().trim(),
                request.email().trim(),
                false,
                true,
                now,
                now);
        employeeRepository.save(employee);
        emailService.sendPasswordEmail(employee.getEmail(), "新建員工帳號通知", generatedPassword);
    }

    public ImportEmployeesResponse importEmployees(MultipartFile file) {
        if (file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "請上傳 CSV 檔案");
        }

        int successCount = 0;
        List<ImportedEmployeeError> errors = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
                CSVParser parser = CSVFormat.DEFAULT
                        .builder()
                        .setHeader()
                        .setSkipHeaderRecord(true)
                        .setIgnoreHeaderCase(true)
                        .setTrim(true)
                        .build()
                        .parse(reader)) {
            for (CSVRecord record : parser) {
                String rawData = String.join(",", record);
                try {
                    String username = getRequired(record, "username");
                    String name = getRequired(record, "name");
                    String email = getRequired(record, "email");
                    Department department = departmentService.getActiveDepartmentByName(
                            getRequired(record, "department"));
                    validateUnique(username, email);
                    String generatedPassword = passwordPolicyService.generateTemporaryPassword();
                    Instant now = Instant.now();
                    Employee employee = new Employee(
                            null,
                            department.getId(),
                            department.getName(),
                            username,
                            passwordPolicyService.hash(generatedPassword),
                            name,
                            email,
                            false,
                            true,
                            now,
                            now);
                    employeeRepository.save(employee);
                    emailService.sendPasswordEmail(email, "新建員工帳號通知", generatedPassword);
                    successCount++;
                } catch (ApiException exception) {
                    errors.add(new ImportedEmployeeError(
                            Math.toIntExact(record.getRecordNumber() + 1),
                            rawData,
                            exception.getMessage()));
                }
            }
        } catch (IOException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "CSV 讀取失敗");
        }

        return new ImportEmployeesResponse(
                "CSV 匯入完成",
                successCount,
                errors.size(),
                errors);
    }

    public EmployeeSummaryResponse updateEmployee(Long employeeId, UpdateEmployeeRequest request) {
        Employee employee = getRequiredEmployee(employeeId);
        String username = request.username().trim();
        String name = request.name().trim();
        String email = request.email().trim();
        validateUniqueForUpdate(employeeId, username, email);
        Department department = departmentService.getActiveDepartment(request.departmentId());

        employee.setUsername(username);
        employee.setName(name);
        employee.setEmail(email);
        employee.setDepartmentId(department.getId());
        employee.setDepartmentName(department.getName());
        employee.setAdmin(Boolean.TRUE.equals(request.isAdmin()));
        employee.touchUpdatedAt(Instant.now());
        employeeRepository.save(employee);
        return toSummary(employee);
    }

    public void updateStatus(Long employeeId, EmployeeStatusRequest request) {
        Employee employee = getRequiredEmployee(employeeId);
        employee.setActive(request.isActive());
        employee.touchUpdatedAt(Instant.now());
        employeeRepository.save(employee);
    }

    public void resetPassword(
            Long employeeId,
            ResetEmployeePasswordRequest request) {
        Employee employee = getRequiredEmployee(employeeId);
        passwordPolicyService.validatePassword(request.newPassword());
        employee.setPasswordHash(passwordPolicyService.hash(request.newPassword()));
        employee.touchUpdatedAt(Instant.now());
        employeeRepository.save(employee);
        refreshTokenService.revokeAllByEmployeeId(employee.getId());
        emailService.sendPasswordEmail(employee.getEmail(), "員工密碼已重設", request.newPassword());
    }

    private EmployeeSummaryResponse toSummary(Employee employee) {
        return new EmployeeSummaryResponse(
                employee.getId(),
                employee.getUsername(),
                employee.getName(),
                employee.getEmail(),
                new DepartmentSummaryResponse(
                        employee.getDepartmentId(),
                        employee.getDepartmentName(),
                        null),
                employee.isAdmin(),
                employee.isActive(),
                employee.getCreatedAt(),
                employee.getUpdatedAt());
    }

    private void validateUnique(String username, String email) {
        if (employeeRepository.existsByUsername(username.trim())) {
            throw new ApiException(HttpStatus.CONFLICT, "username 已存在");
        }
        if (employeeRepository.existsByEmail(email.trim())) {
            throw new ApiException(HttpStatus.CONFLICT, "Email 已存在");
        }
    }

    private void validateUniqueForUpdate(Long employeeId, String username, String email) {
        employeeRepository.findByUsername(username)
                .filter(existing -> !existing.getId().equals(employeeId))
                .ifPresent(existing -> {
                    throw new ApiException(HttpStatus.CONFLICT, "username 已被其他員工使用");
                });
        employeeRepository.findByEmail(email)
                .filter(existing -> !existing.getId().equals(employeeId))
                .ifPresent(existing -> {
                    throw new ApiException(HttpStatus.CONFLICT, "Email 已被其他員工使用");
                });
    }

    private Employee getRequiredEmployee(Long employeeId) {
        return employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "找不到員工"));
    }

    private String getRequired(CSVRecord record, String key) {
        String value = record.get(key);
        if (value == null || value.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, key + " 為必填");
        }
        return value.trim();
    }
}
