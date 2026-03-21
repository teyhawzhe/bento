package com.lovius.bento.dao;

import com.lovius.bento.model.ErrorNotificationEmail;
import java.util.List;
import java.util.Optional;

public interface ErrorNotificationEmailRepository {
    List<ErrorNotificationEmail> findAll();
    Optional<ErrorNotificationEmail> findById(Long id);
    Optional<ErrorNotificationEmail> findByEmail(String email);
    boolean existsByEmail(String email);
    ErrorNotificationEmail save(ErrorNotificationEmail errorNotificationEmail);
    void deleteById(Long id);
}
