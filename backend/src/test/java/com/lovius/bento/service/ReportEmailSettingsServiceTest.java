package com.lovius.bento.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lovius.bento.dao.ReportRecipientEmailRepository;
import com.lovius.bento.dto.CreateReportEmailRequest;
import com.lovius.bento.exception.ApiException;
import com.lovius.bento.model.ReportRecipientEmail;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReportEmailSettingsServiceTest {
    @Mock
    private ReportRecipientEmailRepository reportRecipientEmailRepository;

    private ReportEmailSettingsService reportEmailSettingsService;

    @BeforeEach
    void setUp() {
        reportEmailSettingsService = new ReportEmailSettingsService(reportRecipientEmailRepository);
    }

    @Test
    void createRejectsDuplicateEmail() {
        when(reportRecipientEmailRepository.existsByEmail("finance@company.local")).thenReturn(true);

        ApiException exception = assertThrows(
                ApiException.class,
                () -> reportEmailSettingsService.create(1L, new CreateReportEmailRequest("finance@company.local")));

        assertEquals("報表收件 Email 已存在", exception.getMessage());
    }

    @Test
    void createNormalizesAndPersistsEmail() {
        reportEmailSettingsService.create(1L, new CreateReportEmailRequest(" Finance@Company.Local "));

        ArgumentCaptor<ReportRecipientEmail> captor = ArgumentCaptor.forClass(ReportRecipientEmail.class);
        verify(reportRecipientEmailRepository).save(captor.capture());
        assertEquals("finance@company.local", captor.getValue().getEmail());
    }

    @Test
    void deleteRejectsMissingRecord() {
        ApiException exception = assertThrows(ApiException.class, () -> reportEmailSettingsService.delete(99L));

        assertEquals("查無報表收件信箱", exception.getMessage());
    }

    @Test
    void providerReturnsConfiguredRecipientEmails() {
        when(reportRecipientEmailRepository.findAll()).thenReturn(List.of(
                new ReportRecipientEmail(1L, "finance@company.local", 1L, Instant.now()),
                new ReportRecipientEmail(2L, "audit@company.local", 1L, Instant.now())));

        List<String> emails = reportEmailSettingsService.getRecipientEmails();

        assertEquals(List.of("finance@company.local", "audit@company.local"), emails);
    }

    @Test
    void deleteRemovesExistingRecord() {
        when(reportRecipientEmailRepository.findById(5L))
                .thenReturn(Optional.of(new ReportRecipientEmail(5L, "finance@company.local", 1L, Instant.now())));

        reportEmailSettingsService.delete(5L);

        verify(reportRecipientEmailRepository).deleteById(5L);
    }
}
