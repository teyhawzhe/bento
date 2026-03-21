package com.lovius.bento.service;

import com.lovius.bento.dao.MonthlyBillingLogRepository;
import com.lovius.bento.dao.MonthlyBillingReportRepository;
import com.lovius.bento.dto.MonthlyBillingLogResponse;
import com.lovius.bento.dto.MonthlyBillingTriggerResponse;
import com.lovius.bento.model.MonthlyBillingAggregationRow;
import com.lovius.bento.model.MonthlyBillingLog;
import com.lovius.bento.model.MonthlyBillingLogView;
import com.lovius.bento.model.MonthlyBillingPeriod;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class MonthlyBillingService {
    private final MonthlyBillingReportRepository monthlyBillingReportRepository;
    private final MonthlyBillingLogRepository monthlyBillingLogRepository;
    private final MonthlyBillingPeriodService monthlyBillingPeriodService;
    private final BillingReportRecipientProvider billingReportRecipientProvider;
    private final EmailService emailService;

    public MonthlyBillingService(
            MonthlyBillingReportRepository monthlyBillingReportRepository,
            MonthlyBillingLogRepository monthlyBillingLogRepository,
            MonthlyBillingPeriodService monthlyBillingPeriodService,
            BillingReportRecipientProvider billingReportRecipientProvider,
            EmailService emailService) {
        this.monthlyBillingReportRepository = monthlyBillingReportRepository;
        this.monthlyBillingLogRepository = monthlyBillingLogRepository;
        this.monthlyBillingPeriodService = monthlyBillingPeriodService;
        this.billingReportRecipientProvider = billingReportRecipientProvider;
        this.emailService = emailService;
    }

    public MonthlyBillingTriggerResponse runMonthlyBilling(Long triggeredBy) {
        MonthlyBillingPeriod period = monthlyBillingPeriodService.currentPeriod();
        return runMonthlyBilling(period, triggeredBy);
    }

    MonthlyBillingTriggerResponse runMonthlyBilling(MonthlyBillingPeriod period, Long triggeredBy) {
        List<MonthlyBillingAggregationRow> rows = monthlyBillingReportRepository.findBillingRows(
                period.startDate(),
                period.endDate());
        Map<Long, SupplierBillingReport> reportsBySupplier = groupReports(rows);

        int recipientCount = 0;
        int failedCount = 0;
        for (SupplierBillingReport report : reportsBySupplier.values()) {
            String subject = buildSubject(report, period);
            String body = buildBody(report, period);
            Set<String> recipients = new LinkedHashSet<>();
            recipients.add(report.supplierEmail());
            recipients.addAll(billingReportRecipientProvider.getRecipientEmails());

            for (String recipient : recipients) {
                recipientCount += 1;
                try {
                    emailService.sendEmail(recipient, subject, body);
                    monthlyBillingLogRepository.save(new MonthlyBillingLog(
                            null,
                            period.startDate(),
                            period.endDate(),
                            report.supplierId(),
                            recipient,
                            "sent",
                            null,
                            triggeredBy,
                            Instant.now(),
                            Instant.now()));
                } catch (RuntimeException exception) {
                    failedCount += 1;
                    monthlyBillingLogRepository.save(new MonthlyBillingLog(
                            null,
                            period.startDate(),
                            period.endDate(),
                            report.supplierId(),
                            recipient,
                            "failed",
                            exception.getMessage(),
                            triggeredBy,
                            null,
                            Instant.now()));
                }
            }
        }

        return new MonthlyBillingTriggerResponse(
                "月結帳單報表已完成處理",
                period.startDate(),
                period.endDate(),
                reportsBySupplier.size(),
                recipientCount,
                failedCount);
    }

    public List<MonthlyBillingLogResponse> getLogs() {
        return monthlyBillingLogRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private Map<Long, SupplierBillingReport> groupReports(List<MonthlyBillingAggregationRow> rows) {
        Map<Long, SupplierBillingReport> reports = new LinkedHashMap<>();
        for (MonthlyBillingAggregationRow row : rows) {
            SupplierBillingReport report = reports.computeIfAbsent(
                    row.supplierId(),
                    ignored -> new SupplierBillingReport(
                            row.supplierId(),
                            row.supplierName(),
                            row.supplierEmail(),
                            new ArrayList<>(),
                            BigDecimal.ZERO));
            SupplierBillingLineItem lineItem = new SupplierBillingLineItem(
                    row.menuName(),
                    row.quantity(),
                    row.subtotal());
            report.lineItems().add(lineItem);
            report.totalAmount = report.totalAmount().add(lineItem.subtotal());
        }
        return reports;
    }

    private String buildSubject(SupplierBillingReport report, MonthlyBillingPeriod period) {
        return "%s 月結帳單 %s ~ %s".formatted(
                report.supplierName(),
                period.startDate(),
                period.endDate());
    }

    private String buildBody(SupplierBillingReport report, MonthlyBillingPeriod period) {
        StringBuilder builder = new StringBuilder();
        builder.append("廠商：").append(report.supplierName()).append("\n");
        builder.append("帳單期間：")
                .append(period.startDate())
                .append(" ～ ")
                .append(period.endDate())
                .append("\n\n");
        builder.append("| 便當名稱 | 數量 | 小計 |\n");
        builder.append("|---------|------|------|\n");
        for (SupplierBillingLineItem lineItem : report.lineItems()) {
            builder.append("| ")
                    .append(lineItem.menuName())
                    .append(" | ")
                    .append(lineItem.quantity())
                    .append(" | ")
                    .append(lineItem.subtotal().stripTrailingZeros().toPlainString())
                    .append(" |\n");
        }
        builder.append("| TOTAL |  | ")
                .append(report.totalAmount().stripTrailingZeros().toPlainString())
                .append(" |\n");
        return builder.toString();
    }

    private MonthlyBillingLogResponse toResponse(MonthlyBillingLogView view) {
        return new MonthlyBillingLogResponse(
                view.id(),
                view.billingPeriodStart(),
                view.billingPeriodEnd(),
                view.supplierId(),
                view.supplierName(),
                view.emailTo(),
                view.status(),
                view.errorMessage(),
                view.triggeredBy(),
                view.sentAt(),
                view.createdAt());
    }

    private static final class SupplierBillingReport {
        private final Long supplierId;
        private final String supplierName;
        private final String supplierEmail;
        private final List<SupplierBillingLineItem> lineItems;
        private BigDecimal totalAmount;

        private SupplierBillingReport(
                Long supplierId,
                String supplierName,
                String supplierEmail,
                List<SupplierBillingLineItem> lineItems,
                BigDecimal totalAmount) {
            this.supplierId = supplierId;
            this.supplierName = supplierName;
            this.supplierEmail = supplierEmail;
            this.lineItems = lineItems;
            this.totalAmount = totalAmount;
        }

        private Long supplierId() {
            return supplierId;
        }

        private String supplierName() {
            return supplierName;
        }

        private String supplierEmail() {
            return supplierEmail;
        }

        private List<SupplierBillingLineItem> lineItems() {
            return lineItems;
        }

        private BigDecimal totalAmount() {
            return totalAmount;
        }
    }

    private record SupplierBillingLineItem(String menuName, long quantity, BigDecimal subtotal) {}
}
