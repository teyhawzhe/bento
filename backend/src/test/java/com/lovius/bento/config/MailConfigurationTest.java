package com.lovius.bento.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import com.lovius.bento.service.AppMailSender;
import com.lovius.bento.service.EmailService;
import com.lovius.bento.service.MockMailSender;
import com.lovius.bento.service.SmtpMailSender;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;

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
