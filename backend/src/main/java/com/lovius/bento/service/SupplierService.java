package com.lovius.bento.service;

import com.lovius.bento.dao.SupplierRepository;
import com.lovius.bento.dto.CreateSupplierRequest;
import com.lovius.bento.dto.SupplierResponse;
import com.lovius.bento.dto.UpdateSupplierRequest;
import com.lovius.bento.exception.ApiException;
import com.lovius.bento.model.Supplier;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
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

    public List<SupplierResponse> getSuppliers(String name, String searchType) {
        String trimmedName = name == null ? "" : name.trim();
        if (trimmedName.isEmpty()) {
            return supplierRepository.findAll().stream().map(this::toResponse).toList();
        }

        String normalizedSearchType = searchType == null ? "exact" : searchType.trim().toLowerCase();
        List<Supplier> suppliers = switch (normalizedSearchType) {
            case "exact" -> supplierRepository.findByNameExact(trimmedName);
            case "fuzzy" -> supplierRepository.findByNameFuzzy(trimmedName);
            default -> throw new ApiException(HttpStatus.BAD_REQUEST, "search_type 僅支援 exact 或 fuzzy");
        };
        return suppliers.stream().map(this::toResponse).toList();
    }

    public SupplierResponse getSupplier(Long supplierId) {
        return toResponse(getRequiredSupplier(supplierId));
    }

    public SupplierResponse updateSupplier(Long supplierId, UpdateSupplierRequest request) {
        Supplier existing = getRequiredSupplier(supplierId);
        Supplier updated = new Supplier(
                existing.getId(),
                request.name().trim(),
                request.email().trim(),
                request.phone().trim(),
                request.contactPerson().trim(),
                existing.getBusinessRegistrationNo(),
                request.isActive(),
                existing.getCreatedAt());
        supplierRepository.save(updated);
        return toResponse(updated);
    }

    private Supplier getRequiredSupplier(Long supplierId) {
        return supplierRepository.findById(supplierId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "查無供應商"));
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
