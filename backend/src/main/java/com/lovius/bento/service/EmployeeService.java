package com.lovius.bento.service;

import com.lovius.bento.dao.EmployeeRepository;
import com.lovius.bento.dto.CreateEmployeeRequest;
import com.lovius.bento.dto.EmployeeCreatedResponse;
import com.lovius.bento.dto.EmployeeSummaryResponse;
import com.lovius.bento.dto.EmployeeStatusRequest;
import com.lovius.bento.dto.ImportEmployeesResponse;
import com.lovius.bento.dto.ImportedEmployeeError;
import com.lovius.bento.dto.ResetEmployeePasswordRequest;
import com.lovius.bento.exception.ApiException;
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
    private final PasswordPolicyService passwordPolicyService;
    private final EmailService emailService;

    public EmployeeService(
            EmployeeRepository employeeRepository,
            PasswordPolicyService passwordPolicyService,
            EmailService emailService) {
        this.employeeRepository = employeeRepository;
        this.passwordPolicyService = passwordPolicyService;
        this.emailService = emailService;
    }

    public List<EmployeeSummaryResponse> getAllEmployees() {
        return employeeRepository.findAll()
                .stream()
                .map(this::toSummary)
                .toList();
    }

    public EmployeeCreatedResponse createEmployee(CreateEmployeeRequest request) {
        validateUnique(request.username(), request.email());
        String generatedPassword = passwordPolicyService.generateTemporaryPassword();
        Instant now = Instant.now();
        Employee employee = new Employee(
                null,
                request.username().trim(),
                passwordPolicyService.hash(generatedPassword),
                request.name().trim(),
                request.email().trim(),
                false,
                true,
                now,
                now);
        employeeRepository.save(employee);
        emailService.sendPasswordEmail(employee.getEmail(), "初始密碼通知", generatedPassword);
        return new EmployeeCreatedResponse("員工帳號建立成功", toSummary(employee), generatedPassword);
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
                    validateUnique(username, email);
                    String generatedPassword = passwordPolicyService.generateTemporaryPassword();
                    Instant now = Instant.now();
                    Employee employee = new Employee(
                            null,
                            username,
                            passwordPolicyService.hash(generatedPassword),
                            name,
                            email,
                            false,
                            true,
                            now,
                            now);
                    employeeRepository.save(employee);
                    emailService.sendPasswordEmail(email, "初始密碼通知", generatedPassword);
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

    public EmployeeSummaryResponse updateStatus(Long employeeId, EmployeeStatusRequest request) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "查無員工"));
        employee.setActive(request.isActive());
        employee.touchUpdatedAt(Instant.now());
        employeeRepository.save(employee);
        return toSummary(employee);
    }

    public EmployeeSummaryResponse resetPassword(
            Long employeeId,
            ResetEmployeePasswordRequest request) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "查無員工"));
        passwordPolicyService.validatePassword(request.newPassword());
        employee.setPasswordHash(passwordPolicyService.hash(request.newPassword()));
        employee.touchUpdatedAt(Instant.now());
        employeeRepository.save(employee);
        emailService.sendPasswordEmail(employee.getEmail(), "密碼重設通知", request.newPassword());
        return toSummary(employee);
    }

    private EmployeeSummaryResponse toSummary(Employee employee) {
        return new EmployeeSummaryResponse(
                employee.getId(),
                employee.getUsername(),
                employee.getName(),
                employee.getEmail(),
                employee.isAdmin(),
                employee.isActive(),
                employee.getCreatedAt(),
                employee.getUpdatedAt());
    }

    private void validateUnique(String username, String email) {
        if (employeeRepository.existsByUsername(username.trim())) {
            throw new ApiException(HttpStatus.CONFLICT, "帳號已存在");
        }
        if (employeeRepository.existsByEmail(email.trim())) {
            throw new ApiException(HttpStatus.CONFLICT, "Email 已存在");
        }
    }

    private String getRequired(CSVRecord record, String key) {
        String value = record.get(key);
        if (value == null || value.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, key + " 欄位不可為空白");
        }
        return value.trim();
    }
}
