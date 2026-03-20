package com.lovius.bento.config;

import com.lovius.bento.dao.EmployeeRepository;
import com.lovius.bento.model.Employee;
import com.lovius.bento.service.PasswordPolicyService;
import java.time.Instant;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DemoDataInitializer {

    @Bean
    public ApplicationRunner seedEmployees(
            EmployeeRepository employeeRepository,
            PasswordPolicyService passwordPolicyService) {
        return arguments -> {
            seedEmployee(
                    employeeRepository,
                    passwordPolicyService,
                    "alice",
                    "WelcomeA1",
                    "Alice Chen",
                    "alice@company.local",
                    false,
                    true);
            seedEmployee(
                    employeeRepository,
                    passwordPolicyService,
                    "admin",
                    "AdminPassA1",
                    "System Admin",
                    "admin@company.local",
                    true,
                    true);
            seedEmployee(
                    employeeRepository,
                    passwordPolicyService,
                    "disabled.user",
                    "DisabledA1",
                    "Disabled User",
                    "disabled@company.local",
                    false,
                    false);
        };
    }

    private void seedEmployee(
            EmployeeRepository employeeRepository,
            PasswordPolicyService passwordPolicyService,
            String username,
            String rawPassword,
            String name,
            String email,
            boolean isAdmin,
            boolean isActive) {
        if (employeeRepository.existsByUsername(username)) {
            return;
        }
        Instant now = Instant.now();
        employeeRepository.save(new Employee(
                null,
                username,
                passwordPolicyService.hash(rawPassword),
                name,
                email,
                isAdmin,
                isActive,
                now,
                now));
    }
}
