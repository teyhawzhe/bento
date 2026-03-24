package com.lovius.bento.service;

import com.lovius.bento.dao.DepartmentRepository;
import com.lovius.bento.dao.EmployeeRepository;
import com.lovius.bento.dto.CreateDepartmentRequest;
import com.lovius.bento.dto.DepartmentSummaryResponse;
import com.lovius.bento.dto.UpdateDepartmentRequest;
import com.lovius.bento.exception.ApiException;
import com.lovius.bento.model.Department;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class DepartmentService {
    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;

    public DepartmentService(
            DepartmentRepository departmentRepository,
            EmployeeRepository employeeRepository) {
        this.departmentRepository = departmentRepository;
        this.employeeRepository = employeeRepository;
    }

    public List<DepartmentSummaryResponse> getAllDepartments() {
        return departmentRepository.findAll().stream().map(this::toSummary).toList();
    }

    public DepartmentSummaryResponse createDepartment(CreateDepartmentRequest request) {
        String name = normalizeName(request.name());
        validateUnique(name, null);
        Instant now = Instant.now();
        Department department = new Department(null, name, true, now, now);
        departmentRepository.save(department);
        return toSummary(department);
    }

    public DepartmentSummaryResponse updateDepartment(Long departmentId, UpdateDepartmentRequest request) {
        Department department = getRequiredDepartment(departmentId);
        String name = normalizeName(request.name());
        validateUnique(name, departmentId);
        department.setName(name);
        department.touchUpdatedAt(Instant.now());
        departmentRepository.save(department);
        return toSummary(department);
    }

    public Department getActiveDepartment(Long departmentId) {
        Department department = getRequiredDepartment(departmentId);
        if (!department.isActive()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "部門已停用");
        }
        return department;
    }

    public Department getActiveDepartmentByName(String name) {
        Department department = departmentRepository.findByName(normalizeName(name))
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "找不到對應部門"));
        if (!department.isActive()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "部門已停用");
        }
        return department;
    }

    private Department getRequiredDepartment(Long departmentId) {
        return departmentRepository.findById(departmentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "找不到部門"));
    }

    private void validateUnique(String name, Long currentDepartmentId) {
        departmentRepository.findByName(name).ifPresent(existing -> {
            if (currentDepartmentId == null || !existing.getId().equals(currentDepartmentId)) {
                throw new ApiException(HttpStatus.CONFLICT, "部門名稱已存在");
            }
        });
    }

    private String normalizeName(String name) {
        return name.trim();
    }

    private DepartmentSummaryResponse toSummary(Department department) {
        return new DepartmentSummaryResponse(
                department.getId(),
                department.getName(),
                department.getCreatedAt());
    }
}
