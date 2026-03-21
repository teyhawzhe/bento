package com.lovius.bento.service;

import com.lovius.bento.dao.SupplierRepository;
import com.lovius.bento.dto.CreateSupplierRequest;
import com.lovius.bento.dto.SupplierResponse;
import com.lovius.bento.model.Supplier;
import java.time.Instant;
import org.springframework.stereotype.Service;

@Service
public class SupplierService {
    private final SupplierRepository supplierRepository;

    public SupplierService(SupplierRepository supplierRepository) {
        this.supplierRepository = supplierRepository;
    }

    public SupplierResponse createSupplier(CreateSupplierRequest request) {
        Supplier supplier = new Supplier(
                null,
                request.name().trim(),
                request.email().trim(),
                request.phone().trim(),
                request.contactPerson().trim(),
                request.businessRegistrationNo().trim(),
                true,
                Instant.now());
        supplierRepository.save(supplier);
        return toResponse(supplier);
    }

    public SupplierResponse toResponse(Supplier supplier) {
        return new SupplierResponse(
                supplier.getId(),
                supplier.getName(),
                supplier.getEmail(),
                supplier.getPhone(),
                supplier.getContactPerson(),
                supplier.getBusinessRegistrationNo(),
                supplier.isActive(),
                supplier.getCreatedAt());
    }
}
