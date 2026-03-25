package com.lovius.bento.dao;

import com.lovius.bento.model.Supplier;
import java.util.List;
import java.util.Optional;

public interface SupplierRepository {
    Supplier save(Supplier supplier);
    Optional<Supplier> findById(Long id);
    List<Supplier> findAll();
    List<Supplier> findByNameExact(String name);
    List<Supplier> findByNameFuzzy(String keyword);
    Optional<Supplier> findByBusinessRegistrationNo(String businessRegistrationNo);
}
