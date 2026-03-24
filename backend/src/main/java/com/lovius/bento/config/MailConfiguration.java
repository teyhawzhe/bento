package com.lovius.bento.config;

import com.lovius.bento.service.AppMailSender;
import com.lovius.bento.service.MockMailSender;
import com.lovius.bento.service.SmtpMailSender;
import java.util.Properties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

@Configuration
@EnableConfigurationProperties(AppMailProperties.class)
public class MailConfiguration {
    @Bean
    public AppMailSender appMailSender(AppMailProperties properties) {
        if (properties.getMode() == AppMailProperties.Mode.SMTP) {
            return new SmtpMailSender(javaMailSender(properties), properties.getFrom());
        }
        return new MockMailSender();
    }

    private JavaMailSender javaMailSender(AppMailProperties properties) {
        AppMailProperties.Smtp smtp = properties.getSmtp();
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(smtp.getHost());
        sender.setPort(smtp.getPort());
        sender.setUsername(smtp.getUsername());
        sender.setPassword(smtp.getPassword());

        Properties mailProperties = sender.getJavaMailProperties();
        mailProperties.put("mail.smtp.auth", Boolean.toString(smtp.isAuth()));
        mailProperties.put("mail.smtp.starttls.enable", Boolean.toString(smtp.isStarttls()));
        mailProperties.put("mail.transport.protocol", "smtp");
        return sender;
    }
}
