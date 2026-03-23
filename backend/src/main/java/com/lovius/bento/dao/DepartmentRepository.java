package com.lovius.bento.dao;

import com.lovius.bento.model.Department;
import java.util.List;
import java.util.Optional;

public interface DepartmentRepository {
    List<Department> findAll();

    Optional<Department> findById(Long id);

    Optional<Department> findByName(String name);

    Department save(Department department);
}
