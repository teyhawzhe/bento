package com.lovius.bento.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lovius.bento.dao.ErrorNotificationEmailRepository;
import com.lovius.bento.dto.CreateErrorEmailRequest;
import com.lovius.bento.dto.ErrorEmailResponse;
import com.lovius.bento.exception.ApiException;
import com.lovius.bento.model.ErrorNotificationEmail;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ErrorEmailSettingsServiceTest {
    @Mock
    private ErrorNotificationEmailRepository errorNotificationEmailRepository;

    private ErrorEmailSettingsService errorEmailSettingsService;

    @BeforeEach
    void setUp() {
        errorEmailSettingsService = new ErrorEmailSettingsService(errorNotificationEmailRepository);
    }

    @Test
    void createNormalizesEmailAndReturnsCreatedResponse() {
        when(errorNotificationEmailRepository.existsByEmail("ops-alerts@company.local")).thenReturn(false);

        ErrorEmailResponse response = errorEmailSettingsService.create(
                9L,
                new CreateErrorEmailRequest("  OPS-ALERTS@company.local  "));

        assertEquals("ops-alerts@company.local", response.email());
        assertEquals(9L, response.createdBy());
        verify(errorNotificationEmailRepository).save(org.mockito.ArgumentMatchers.argThat(saved ->
                "ops-alerts@company.local".equals(saved.getEmail()) && Long.valueOf(9L).equals(saved.getCreatedBy())));
    }

    @Test
    void createRejectsDuplicateEmail() {
        when(errorNotificationEmailRepository.existsByEmail("ops-alerts@company.local")).thenReturn(true);

        ApiException exception = assertThrows(
                ApiException.class,
                () -> errorEmailSettingsService.create(9L, new CreateErrorEmailRequest("ops-alerts@company.local")));

        assertEquals("錯誤通知 Email 已存在", exception.getMessage());
    }

    @Test
    void deleteRejectsMissingRecord() {
        when(errorNotificationEmailRepository.findById(44L)).thenReturn(Optional.empty());

        ApiException exception = assertThrows(
                ApiException.class,
                () -> errorEmailSettingsService.delete(44L));

        assertEquals("查無錯誤通知信箱", exception.getMessage());
    }

    @Test
    void providerReturnsEmailsForA003Reuse() {
        when(errorNotificationEmailRepository.findAll()).thenReturn(List.of(
                new ErrorNotificationEmail(1L, "ops-a@company.local", 1L, Instant.now()),
                new ErrorNotificationEmail(2L, "ops-b@company.local", 1L, Instant.now())));

        List<String> recipients = errorEmailSettingsService.getRecipientEmails();

        assertEquals(List.of("ops-a@company.local", "ops-b@company.local"), recipients);
    }
}
