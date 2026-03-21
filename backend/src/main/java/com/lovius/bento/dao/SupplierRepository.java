package com.lovius.bento.dao;

import com.lovius.bento.model.Supplier;
import java.util.List;
import java.util.Optional;

public interface SupplierRepository {
    Supplier save(Supplier supplier);
    Optional<Supplier> findById(Long id);
    List<Supplier> findAll();
}
