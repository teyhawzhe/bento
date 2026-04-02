package com.lovius.bento.service;

import com.lovius.bento.dao.MenuNotificationDismissRepository;
import com.lovius.bento.dao.MenuNotificationLogRepository;
import com.lovius.bento.dao.MenuRepository;
import com.lovius.bento.dao.WorkCalendarRepository;
import com.lovius.bento.model.Menu;
import com.lovius.bento.model.MenuNotificationDismiss;
import com.lovius.bento.model.MenuNotificationLog;
import com.lovius.bento.model.WorkCalendar;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MenuCheckNotificationService {
    private static final Logger logger = LoggerFactory.getLogger(MenuCheckNotificationService.class);
    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Taipei");

    private final WorkCalendarRepository workCalendarRepository;
    private final MenuRepository menuRepository;
    private final MenuNotificationLogRepository menuNotificationLogRepository;
    private final MenuNotificationDismissRepository menuNotificationDismissRepository;
    private final ErrorNotificationRecipientProvider errorNotificationRecipientProvider;
    private final EmailService emailService;
    private final Clock clock;

    @Autowired
    public MenuCheckNotificationService(
            WorkCalendarRepository workCalendarRepository,
            MenuRepository menuRepository,
            MenuNotificationLogRepository menuNotificationLogRepository,
            MenuNotificationDismissRepository menuNotificationDismissRepository,
            ErrorNotificationRecipientProvider errorNotificationRecipientProvider,
            EmailService emailService) {
        this(
                workCalendarRepository,
                menuRepository,
                menuNotificationLogRepository,
                menuNotificationDismissRepository,
                errorNotificationRecipientProvider,
                emailService,
                Clock.system(ZONE_ID));
    }

    MenuCheckNotificationService(
            WorkCalendarRepository workCalendarRepository,
            MenuRepository menuRepository,
            MenuNotificationLogRepository menuNotificationLogRepository,
            MenuNotificationDismissRepository menuNotificationDismissRepository,
            ErrorNotificationRecipientProvider errorNotificationRecipientProvider,
            EmailService emailService,
            Clock clock) {
        this.workCalendarRepository = workCalendarRepository;
        this.menuRepository = menuRepository;
        this.menuNotificationLogRepository = menuNotificationLogRepository;
        this.menuNotificationDismissRepository = menuNotificationDismissRepository;
        this.errorNotificationRecipientProvider = errorNotificationRecipientProvider;
        this.emailService = emailService;
        this.clock = clock;
    }

    public List<LocalDate> getMissingDatesForAdmin() {
        LocalDate today = LocalDate.now(clock);
        if (menuNotificationDismissRepository.existsByDismissDate(today)) {
            return List.of();
        }
        return calculateMissingDates(today);
    }

    public void dismissToday() {
        LocalDate today = LocalDate.now(clock);
        menuNotificationDismissRepository.save(new MenuNotificationDismiss(today, Instant.now(clock)));
    }

    public void runDailyCheck() {
        LocalDate today = LocalDate.now(clock);
        List<LocalDate> missingDates = calculateMissingDates(today);
        if (missingDates.isEmpty()) {
            return;
        }
        if (menuNotificationLogRepository.existsByNotifyDate(today)) {
            return;
        }

        Instant now = Instant.now(clock);
        LocalDate missingFrom = missingDates.getFirst();
        LocalDate missingTo = missingDates.getLast();
        List<String> recipients = errorNotificationRecipientProvider.getRecipientEmails();
        String subject = "A015 菜單設定提醒 %s".formatted(today);
        String body = buildEmailBody(today, missingDates);

        if (recipients.isEmpty()) {
            logger.warn("A015 menu check found missing dates but no error notification recipients configured");
            menuNotificationLogRepository.save(new MenuNotificationLog(
                    null,
                    today,
                    missingFrom,
                    missingTo,
                    "fail",
                    now));
            return;
        }

        try {
            for (String recipient : recipients) {
                emailService.sendEmail(recipient, subject, body);
            }
            menuNotificationLogRepository.save(new MenuNotificationLog(
                    null,
                    today,
                    missingFrom,
                    missingTo,
                    "success",
                    now));
        } catch (RuntimeException exception) {
            logger.error("A015 menu check email sending failed for notifyDate={}", today, exception);
            menuNotificationLogRepository.save(new MenuNotificationLog(
                    null,
                    today,
                    missingFrom,
                    missingTo,
                    "fail",
                    now));
        }
    }

    private List<LocalDate> calculateMissingDates(LocalDate baseDate) {
        LocalDate endDate = baseDate.plusMonths(1);
        List<LocalDate> workdays = workCalendarRepository.findByDateRange(baseDate, endDate).stream()
                .filter(WorkCalendar::isWorkday)
                .map(WorkCalendar::getDate)
                .sorted()
                .toList();
        if (workdays.isEmpty()) {
            return List.of();
        }

        List<Menu> menus = menuRepository.findAll(true, baseDate, null).stream()
                .filter(menu -> !menu.getValidTo().isBefore(baseDate) && !menu.getValidFrom().isAfter(endDate))
                .toList();

        return workdays.stream()
                .filter(date -> menus.stream().noneMatch(menu ->
                        !menu.getValidFrom().isAfter(date) && !menu.getValidTo().isBefore(date)))
                .sorted(Comparator.naturalOrder())
                .toList();
    }

    private String buildEmailBody(LocalDate notifyDate, List<LocalDate> missingDates) {
        return """
                A015 便當菜單設定提醒
                通知日期：%s
                以下上班日尚未設定菜單：
                %s
                """.formatted(
                notifyDate,
                missingDates.stream()
                        .map(LocalDate::toString)
                        .reduce((left, right) -> left + "\n" + right)
                        .orElse(""));
    }
}
