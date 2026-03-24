package com.lovius.bento.service;

import com.lovius.bento.dao.NotificationLogRepository;
import com.lovius.bento.dao.OrderRepository;
import com.lovius.bento.model.NotificationLog;
import com.lovius.bento.model.SupplierOrderNotificationRow;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SupplierOrderNotificationService {
    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Taipei");

    private final OrderRepository orderRepository;
    private final NotificationLogRepository notificationLogRepository;
    private final ErrorNotificationRecipientProvider errorNotificationRecipientProvider;
    private final EmailService emailService;
    private final Clock clock;

    @Autowired
    public SupplierOrderNotificationService(
            OrderRepository orderRepository,
            NotificationLogRepository notificationLogRepository,
            ErrorNotificationRecipientProvider errorNotificationRecipientProvider,
            EmailService emailService) {
        this(
                orderRepository,
                notificationLogRepository,
                errorNotificationRecipientProvider,
                emailService,
                Clock.system(ZONE_ID));
    }

    SupplierOrderNotificationService(
            OrderRepository orderRepository,
            NotificationLogRepository notificationLogRepository,
            ErrorNotificationRecipientProvider errorNotificationRecipientProvider,
            EmailService emailService,
            Clock clock) {
        this.orderRepository = orderRepository;
        this.notificationLogRepository = notificationLogRepository;
        this.errorNotificationRecipientProvider = errorNotificationRecipientProvider;
        this.emailService = emailService;
        this.clock = clock;
    }

    public void runTomorrowNotifications() {
        runNotificationsForDate(LocalDate.now(clock).plusDays(1));
    }

    void runNotificationsForDate(LocalDate notifyDate) {
        try {
            List<SupplierOrderNotificationRow> rows = orderRepository.findSupplierOrderNotificationRows(notifyDate);
            Map<Long, SupplierNotification> notifications = groupNotifications(rows);
            for (SupplierNotification notification : notifications.values()) {
                sendSupplierNotification(notification, notifyDate);
            }
        } catch (RuntimeException exception) {
            String body = buildSystemErrorBody(notifyDate, exception);
            notifySystemErrorRecipients(notifyDate, body, exception);
        }
    }

    private Map<Long, SupplierNotification> groupNotifications(List<SupplierOrderNotificationRow> rows) {
        Map<Long, SupplierNotification> notifications = new LinkedHashMap<>();
        for (SupplierOrderNotificationRow row : rows) {
            SupplierNotification notification = notifications.computeIfAbsent(
                    row.supplierId(),
                    ignored -> new SupplierNotification(
                            row.supplierId(),
                            row.supplierName(),
                            row.supplierEmail(),
                            new ArrayList<>()));
            notification.lineItems().add(new SupplierNotificationLine(row.menuName(), row.quantity()));
        }
        return notifications;
    }

    private void sendSupplierNotification(SupplierNotification notification, LocalDate notifyDate) {
        String body = buildSupplierNotificationBody(notification, notifyDate);
        String subject = "%s 隔日訂單通知 %s".formatted(notification.supplierName(), notifyDate);
        String recipient = notification.supplierEmail() == null ? "" : notification.supplierEmail().trim();
        Instant now = Instant.now(clock);

        if (recipient.isBlank()) {
            notificationLogRepository.save(new NotificationLog(
                    null,
                    notifyDate,
                    recipient,
                    body,
                    "failed",
                    "供應商通知信箱未設定",
                    now));
            notifyErrorRecipients(
                    notifyDate,
                    notification,
                    body,
                    "供應商通知信箱未設定",
                    "failed");
            return;
        }

        try {
            emailService.sendEmail(recipient, subject, body);
            notificationLogRepository.save(new NotificationLog(
                    null,
                    notifyDate,
                    recipient,
                    body,
                    "success",
                    null,
                    now));
        } catch (RuntimeException exception) {
            notificationLogRepository.save(new NotificationLog(
                    null,
                    notifyDate,
                    recipient,
                    body,
                    "exception",
                    exception.getMessage(),
                    now));
            notifyErrorRecipients(
                    notifyDate,
                    notification,
                    body,
                    exception.getMessage(),
                    "exception");
        }
    }

    private void notifyErrorRecipients(
            LocalDate notifyDate,
            SupplierNotification notification,
            String originalContent,
            String errorMessage,
            String errorStatus) {
        String subject = "A003 每日供應商通知異常 %s".formatted(notifyDate);
        String body = """
                隔日供應商通知發送%s
                通知日期：%s
                供應商：%s
                原收件人：%s
                錯誤訊息：%s

                原始通知內容：
                %s
                """.formatted(
                "failed".equals(errorStatus) ? "失敗" : "異常",
                notifyDate,
                notification.supplierName(),
                notification.supplierEmail(),
                errorMessage,
                originalContent);
        notifyRecipients(notifyDate, body, subject, errorMessage, errorStatus);
    }

    private void notifySystemErrorRecipients(LocalDate notifyDate, String body, RuntimeException exception) {
        notifyRecipients(
                notifyDate,
                body,
                "A003 每日供應商通知系統錯誤 %s".formatted(notifyDate),
                exception.getMessage(),
                "system_error");
    }

    private void notifyRecipients(
            LocalDate notifyDate,
            String body,
            String subject,
            String errorMessage,
            String status) {
        List<String> recipients = errorNotificationRecipientProvider.getRecipientEmails();
        if (recipients.isEmpty()) {
            notificationLogRepository.save(new NotificationLog(
                    null,
                    notifyDate,
                    "(no-error-recipients-configured)",
                    body,
                    status,
                    errorMessage,
                    Instant.now(clock)));
            return;
        }

        for (String recipient : recipients) {
            try {
                emailService.sendEmail(recipient, subject, body);
            } catch (RuntimeException ignored) {
                // Preserve the original supplier/system error flow even if the fallback mail also fails.
            }
            notificationLogRepository.save(new NotificationLog(
                    null,
                    notifyDate,
                    recipient,
                    body,
                    status,
                    errorMessage,
                    Instant.now(clock)));
        }
    }

    private String buildSupplierNotificationBody(SupplierNotification notification, LocalDate notifyDate) {
        StringBuilder builder = new StringBuilder();
        builder.append("供應商：").append(notification.supplierName()).append("\n");
        builder.append("出餐日期：").append(notifyDate).append("\n\n");
        builder.append("| 便當名稱 | 數量 |\n");
        builder.append("|---------|------|\n");
        for (SupplierNotificationLine lineItem : notification.lineItems()) {
            builder.append("| ")
                    .append(lineItem.menuName())
                    .append(" | ")
                    .append(lineItem.quantity())
                    .append(" |\n");
        }
        return builder.toString();
    }

    private String buildSystemErrorBody(LocalDate notifyDate, RuntimeException exception) {
        return """
                A003 每日供應商通知執行期間發生系統錯誤
                通知日期：%s
                錯誤訊息：%s
                """.formatted(notifyDate, exception.getMessage());
    }

    private record SupplierNotificationLine(String menuName, long quantity) {}

    private record SupplierNotification(
            Long supplierId,
            String supplierName,
            String supplierEmail,
            List<SupplierNotificationLine> lineItems) {}
}
