package com.lovius.bento.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lovius.bento.service.AppMailSender;
import com.lovius.bento.service.EmailService;
import com.lovius.bento.service.MockMailSender;
import com.lovius.bento.service.SmtpMailSender;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(OutputCaptureExtension.class)
class MailConfigurationTest {
    @Test
    void devProfileUsesMockModeByDefault() {
        try (ConfigurableApplicationContext context = runWithProfile("dev")) {
            AppMailProperties properties = context.getBean(AppMailProperties.class);
            AppMailSender mailSender = context.getBean(AppMailSender.class);
            EmailService emailService = context.getBean(EmailService.class);

            assertEquals(AppMailProperties.Mode.MOCK, properties.getMode());
            assertInstanceOf(MockMailSender.class, mailSender);
            emailService.sendEmail("alice@company.local", "dev", "hello");
            assertEquals(1, emailService.getSentEmails().size());
        }
    }

    @Test
    void mockModeLogsRecipientSubjectAndBody(CapturedOutput output) {
        try (ConfigurableApplicationContext context = runWithProfile("dev")) {
            EmailService emailService = context.getBean(EmailService.class);

            emailService.sendEmail("alice@company.local", "Daily Bento", "Order summary");

            assertEquals(1, emailService.getSentEmails().size());
            assertTrue(output.getOut().contains("MOCK_EMAIL"));
            assertTrue(output.getOut().contains("recipient=alice@company.local"));
            assertTrue(output.getOut().contains("subject=Daily Bento"));
            assertTrue(output.getOut().contains("body="));
            assertTrue(output.getOut().contains("Order summary"));
        }
    }

    @Test
    void stagingProfileUsesSmtpModeByDefault() {
        try (ConfigurableApplicationContext context = runWithProfile("staging")) {
            AppMailProperties properties = context.getBean(AppMailProperties.class);
            AppMailSender mailSender = context.getBean(AppMailSender.class);

            assertEquals(AppMailProperties.Mode.SMTP, properties.getMode());
            assertInstanceOf(SmtpMailSender.class, mailSender);
        }
    }

    @Test
    void productionProfileUsesSmtpModeByDefault() {
        try (ConfigurableApplicationContext context = runWithProfile("production")) {
            AppMailProperties properties = context.getBean(AppMailProperties.class);
            AppMailSender mailSender = context.getBean(AppMailSender.class);

            assertEquals(AppMailProperties.Mode.SMTP, properties.getMode());
            assertInstanceOf(SmtpMailSender.class, mailSender);
        }
    }

    @Test
    void stagingProfileLoadsDefaultSmtpSettings() {
        try (ConfigurableApplicationContext context = runWithProfile("staging")) {
            AppMailProperties properties = context.getBean(AppMailProperties.class);
            AppMailSender mailSender = context.getBean(AppMailSender.class);

            assertEquals(AppMailProperties.Mode.SMTP, properties.getMode());
            assertEquals("smtp.gmail.com", properties.getSmtp().getHost());
            assertEquals(587, properties.getSmtp().getPort());
            assertEquals("staging-mail@lovius.local", properties.getFrom());
            assertInstanceOf(SmtpMailSender.class, mailSender);
        }
    }

    private ConfigurableApplicationContext runWithProfile(String profile) {
        return new SpringApplicationBuilder(TestApplication.class)
                .profiles(profile)
                .properties(
                        "spring.main.banner-mode=off",
                        "spring.main.web-application-type=none")
                .run();
    }

    @SpringBootConfiguration
    @Import({MailConfiguration.class, EmailService.class})
    static class TestApplication {}
}
