package com.lovius.bento.dao;

import com.lovius.bento.model.ReportRecipientEmail;
import java.util.List;
import java.util.Optional;

public interface ReportRecipientEmailRepository {
    List<ReportRecipientEmail> findAll();
    Optional<ReportRecipientEmail> findById(Long id);
    Optional<ReportRecipientEmail> findByEmail(String email);
    boolean existsByEmail(String email);
    ReportRecipientEmail save(ReportRecipientEmail reportRecipientEmail);
    void deleteById(Long id);
}
