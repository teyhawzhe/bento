package com.lovius.bento.config;

import com.lovius.bento.dao.DepartmentRepository;
import com.lovius.bento.dao.EmployeeRepository;
import com.lovius.bento.dao.ErrorNotificationEmailRepository;
import com.lovius.bento.dao.ReportRecipientEmailRepository;
import com.lovius.bento.model.Department;
import com.lovius.bento.model.Employee;
import com.lovius.bento.model.ErrorNotificationEmail;
import com.lovius.bento.model.ReportRecipientEmail;
import com.lovius.bento.service.PasswordPolicyService;
import java.time.Instant;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Configuration
public class DemoDataInitializer {

    @Bean
    @Order(0)
    public ApplicationRunner seedEmployees(
            DepartmentRepository departmentRepository,
            EmployeeRepository employeeRepository,
            PasswordPolicyService passwordPolicyService,
            ErrorNotificationEmailRepository errorNotificationEmailRepository,
            ReportRecipientEmailRepository reportRecipientEmailRepository) {
        return arguments -> {
            seedEmployee(
                    departmentRepository,
                    employeeRepository,
                    passwordPolicyService,
                    "alice",
                    "WelcomeA1",
                    "Alice Chen",
                    "alice@company.local",
                    "Operations",
                    false,
                    true);
            seedEmployee(
                    departmentRepository,
                    employeeRepository,
                    passwordPolicyService,
                    "admin",
                    "AdminPassA1",
                    "System Admin",
                    "admin@company.local",
                    "Management",
                    true,
                    true);
            seedEmployee(
                    departmentRepository,
                    employeeRepository,
                    passwordPolicyService,
                    "disabled.user",
                    "DisabledA1",
                    "Disabled User",
                    "disabled@company.local",
                    "Operations",
                    false,
                    false);

            seedA004Data(employeeRepository, errorNotificationEmailRepository);
            seedA008Data(employeeRepository, reportRecipientEmailRepository);
        };
    }

    private void seedEmployee(
            DepartmentRepository departmentRepository,
            EmployeeRepository employeeRepository,
            PasswordPolicyService passwordPolicyService,
            String username,
            String rawPassword,
            String name,
            String email,
            String departmentName,
            boolean isAdmin,
            boolean isActive) {
        if (employeeRepository.existsByUsername(username)) {
            return;
        }
        Department department = getOrCreateDepartment(departmentRepository, departmentName);
        Instant now = Instant.now();
        employeeRepository.save(new Employee(
                null,
                department.getId(),
                department.getName(),
                username,
                passwordPolicyService.hash(rawPassword),
                name,
                email,
                isAdmin,
                isActive,
                now,
                now));
    }

    private Department getOrCreateDepartment(
            DepartmentRepository departmentRepository,
            String departmentName) {
        return departmentRepository.findByName(departmentName)
                .orElseGet(() -> {
                    Instant now = Instant.now();
                    return departmentRepository.save(new Department(
                            null,
                            departmentName,
                            true,
                            now,
                            now));
                });
    }

    private void seedA004Data(
            EmployeeRepository employeeRepository,
            ErrorNotificationEmailRepository errorNotificationEmailRepository) {
        if (errorNotificationEmailRepository.existsByEmail("alerts@company.local")) {
            return;
        }

        Employee admin = employeeRepository.findByUsername("admin").orElseThrow();
        errorNotificationEmailRepository.save(new ErrorNotificationEmail(
                null,
                "alerts@company.local",
                admin.getId(),
                Instant.now()));
    }

    private void seedA008Data(
            EmployeeRepository employeeRepository,
            ReportRecipientEmailRepository reportRecipientEmailRepository) {
        if (reportRecipientEmailRepository.existsByEmail("billing-ops@company.local")) {
            return;
        }

        Employee admin = employeeRepository.findByUsername("admin").orElseThrow();
        reportRecipientEmailRepository.save(new ReportRecipientEmail(
                null,
                "billing-ops@company.local",
                admin.getId(),
                Instant.now()));
    }
}
