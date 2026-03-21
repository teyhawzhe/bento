package com.lovius.bento.dao;

import com.lovius.bento.model.Menu;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MenuRepository {
    Menu save(Menu menu);
    Optional<Menu> findById(Long id);
    List<Menu> findAll(boolean includeHistory, LocalDate today);
    List<Menu> findAvailableForDate(LocalDate orderDate);
}
