package com.lovius.bento.service;

import com.lovius.bento.dao.ErrorNotificationEmailRepository;
import com.lovius.bento.dto.CreateErrorEmailRequest;
import com.lovius.bento.dto.ErrorEmailResponse;
import com.lovius.bento.exception.ApiException;
import com.lovius.bento.model.ErrorNotificationEmail;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class ErrorEmailSettingsService implements ErrorNotificationRecipientProvider {
    private final ErrorNotificationEmailRepository errorNotificationEmailRepository;

    public ErrorEmailSettingsService(ErrorNotificationEmailRepository errorNotificationEmailRepository) {
        this.errorNotificationEmailRepository = errorNotificationEmailRepository;
    }

    public List<ErrorEmailResponse> getAll() {
        return errorNotificationEmailRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public ErrorEmailResponse create(Long createdBy, CreateErrorEmailRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();
        if (errorNotificationEmailRepository.existsByEmail(normalizedEmail)) {
            throw new ApiException(HttpStatus.CONFLICT, "錯誤通知 Email 已存在");
        }

        ErrorNotificationEmail errorNotificationEmail = new ErrorNotificationEmail(
                null,
                normalizedEmail,
                createdBy,
                Instant.now());
        errorNotificationEmailRepository.save(errorNotificationEmail);
        return toResponse(errorNotificationEmail);
    }

    public void delete(Long id) {
        errorNotificationEmailRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "查無錯誤通知信箱"));
        errorNotificationEmailRepository.deleteById(id);
    }

    @Override
    public List<String> getRecipientEmails() {
        return errorNotificationEmailRepository.findAll()
                .stream()
                .map(ErrorNotificationEmail::getEmail)
                .toList();
    }

    private ErrorEmailResponse toResponse(ErrorNotificationEmail errorNotificationEmail) {
        return new ErrorEmailResponse(
                errorNotificationEmail.getId(),
                errorNotificationEmail.getEmail(),
                errorNotificationEmail.getCreatedBy(),
                errorNotificationEmail.getCreatedAt());
    }
}
