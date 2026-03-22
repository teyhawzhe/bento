package com.lovius.bento.service;

import java.util.List;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Primary
@Service
public class A008BillingReportRecipientProvider implements BillingReportRecipientProvider {
    private final ReportEmailSettingsService reportEmailSettingsService;

    public A008BillingReportRecipientProvider(ReportEmailSettingsService reportEmailSettingsService) {
        this.reportEmailSettingsService = reportEmailSettingsService;
    }

    @Override
    public List<String> getRecipientEmails() {
        return reportEmailSettingsService.getRecipientEmails();
    }
}
