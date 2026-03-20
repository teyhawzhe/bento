package com.lovius.bento.dao;

import com.lovius.bento.model.Employee;
import java.util.List;
import java.util.Optional;

public interface EmployeeRepository {
    List<Employee> findAll();

    Optional<Employee> findById(Long id);

    Optional<Employee> findByUsername(String username);

    Optional<Employee> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    Employee save(Employee employee);
}
