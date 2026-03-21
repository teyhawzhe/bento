package com.lovius.bento.config;

import com.lovius.bento.dao.EmployeeRepository;
import com.lovius.bento.dao.MenuRepository;
import com.lovius.bento.dao.OrderRepository;
import com.lovius.bento.dao.SupplierRepository;
import com.lovius.bento.model.Employee;
import com.lovius.bento.model.Menu;
import com.lovius.bento.model.Supplier;
import com.lovius.bento.model.BentoOrder;
import com.lovius.bento.service.PasswordPolicyService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DemoDataInitializer {

    @Bean
    public ApplicationRunner seedEmployees(
            EmployeeRepository employeeRepository,
            PasswordPolicyService passwordPolicyService,
            SupplierRepository supplierRepository,
            MenuRepository menuRepository,
            OrderRepository orderRepository) {
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

            seedA002Data(employeeRepository, supplierRepository, menuRepository, orderRepository);
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

    private void seedA002Data(
            EmployeeRepository employeeRepository,
            SupplierRepository supplierRepository,
            MenuRepository menuRepository,
            OrderRepository orderRepository) {
        if (!supplierRepository.findAll().isEmpty()) {
            return;
        }

        Supplier supplier = supplierRepository.save(new Supplier(
                null,
                "好食便當",
                "orders@haoshi.local",
                "02-1234-5678",
                "王小明",
                "12345678",
                true,
                Instant.now()));

        Employee admin = employeeRepository.findByUsername("admin").orElseThrow();
        Employee alice = employeeRepository.findByUsername("alice").orElseThrow();
        LocalDate nextMonday = LocalDate.now().plusWeeks(1).with(java.time.DayOfWeek.MONDAY);
        Instant now = Instant.now();

        Menu chicken = menuRepository.save(new Menu(
                null,
                supplier.getId(),
                "香烤雞腿便當",
                "肉類",
                "附三樣配菜",
                new BigDecimal("120.00"),
                nextMonday,
                nextMonday.plusDays(4),
                admin.getId(),
                now,
                now));
        menuRepository.save(new Menu(
                null,
                supplier.getId(),
                "鮭魚排便當",
                "海鮮",
                "附季節時蔬",
                new BigDecimal("135.00"),
                nextMonday,
                nextMonday.plusDays(4),
                admin.getId(),
                now,
                now));
        orderRepository.save(new BentoOrder(
                null,
                alice.getId(),
                chicken.getId(),
                nextMonday,
                alice.getId(),
                now));
    }
}
