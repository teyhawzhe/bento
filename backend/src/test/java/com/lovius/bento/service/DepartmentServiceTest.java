package com.lovius.bento.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lovius.bento.dao.DepartmentRepository;
import com.lovius.bento.dao.EmployeeRepository;
import com.lovius.bento.dto.CreateDepartmentRequest;
import com.lovius.bento.dto.UpdateDepartmentRequest;
import com.lovius.bento.exception.ApiException;
import com.lovius.bento.model.Department;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceTest {
    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    private DepartmentService departmentService;

    @BeforeEach
    void setUp() {
        departmentService = new DepartmentService(departmentRepository, employeeRepository);
    }

    @Test
    void createDepartmentRejectsDuplicateName() {
        when(departmentRepository.findByName("IT"))
                .thenReturn(Optional.of(department(1L, "IT", true)));

        ApiException exception = assertThrows(ApiException.class,
                () -> departmentService.createDepartment(new CreateDepartmentRequest("IT")));

        assertEquals("部門名稱已存在", exception.getMessage());
    }

    @Test
    void updateDepartmentRejectsDeactivateWhenEmployeesExist() {
        when(departmentRepository.findById(3L))
                .thenReturn(Optional.of(department(3L, "HR", true)));
        when(departmentRepository.findByName("HR"))
                .thenReturn(Optional.of(department(3L, "HR", true)));
        when(employeeRepository.existsByDepartmentId(3L)).thenReturn(true);

        ApiException exception = assertThrows(ApiException.class,
                () -> departmentService.updateDepartment(3L, new UpdateDepartmentRequest("HR", false)));

        assertEquals("部門已有員工使用，無法停用", exception.getMessage());
    }

    @Test
    void deactivateDepartmentPersistsInactiveState() {
        Department department = department(5L, "Finance", true);
        when(departmentRepository.findById(5L)).thenReturn(Optional.of(department));
        when(employeeRepository.existsByDepartmentId(5L)).thenReturn(false);

        departmentService.deactivateDepartment(5L);

        assertEquals(false, department.isActive());
        verify(departmentRepository).save(department);
    }

    @Test
    void getAllDepartmentsReturnsRepositoryData() {
        when(departmentRepository.findAll()).thenReturn(List.of(
                department(1L, "Management", true),
                department(2L, "Operations", false)));

        var response = departmentService.getAllDepartments();

        assertEquals(2, response.size());
        assertEquals("Management", response.getFirst().name());
        assertEquals(false, response.get(1).isActive());
    }

    private Department department(Long id, String name, boolean isActive) {
        Instant now = Instant.parse("2026-03-23T08:30:00Z");
        return new Department(id, name, isActive, now, now);
    }
}
