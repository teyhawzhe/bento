package com.lovius.bento.service;

import com.lovius.bento.dao.ReportRecipientEmailRepository;
import com.lovius.bento.dto.CreateReportEmailRequest;
import com.lovius.bento.dto.ReportEmailResponse;
import com.lovius.bento.exception.ApiException;
import com.lovius.bento.model.ReportRecipientEmail;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class ReportEmailSettingsService implements BillingReportRecipientProvider {
    private final ReportRecipientEmailRepository reportRecipientEmailRepository;

    public ReportEmailSettingsService(ReportRecipientEmailRepository reportRecipientEmailRepository) {
        this.reportRecipientEmailRepository = reportRecipientEmailRepository;
    }

    public List<ReportEmailResponse> getAll() {
        return reportRecipientEmailRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public ReportEmailResponse create(Long createdBy, CreateReportEmailRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();
        if (reportRecipientEmailRepository.existsByEmail(normalizedEmail)) {
            throw new ApiException(HttpStatus.CONFLICT, "報表收件 Email 已存在");
        }

        ReportRecipientEmail reportRecipientEmail = new ReportRecipientEmail(
                null,
                normalizedEmail,
                createdBy,
                Instant.now());
        reportRecipientEmailRepository.save(reportRecipientEmail);
        return toResponse(reportRecipientEmail);
    }

    public void delete(Long id) {
        reportRecipientEmailRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "查無報表收件信箱"));
        reportRecipientEmailRepository.deleteById(id);
    }

    @Override
    public List<String> getRecipientEmails() {
        return reportRecipientEmailRepository.findAll()
                .stream()
                .map(ReportRecipientEmail::getEmail)
                .toList();
    }

    private ReportEmailResponse toResponse(ReportRecipientEmail reportRecipientEmail) {
        return new ReportEmailResponse(
                reportRecipientEmail.getId(),
                reportRecipientEmail.getEmail(),
                reportRecipientEmail.getCreatedBy(),
                reportRecipientEmail.getCreatedAt());
    }
}
