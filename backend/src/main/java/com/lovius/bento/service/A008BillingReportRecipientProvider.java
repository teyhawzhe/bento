package com.lovius.bento.service;

import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class A008BillingReportRecipientProvider implements BillingReportRecipientProvider {
    @Override
    public List<String> getRecipientEmails() {
        return List.of("finance-reports@company.local");
    }
}
