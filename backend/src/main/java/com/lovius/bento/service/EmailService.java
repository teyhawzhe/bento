package com.lovius.bento.service;

import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    private final AppMailSender appMailSender;

    public EmailService(AppMailSender appMailSender) {
        this.appMailSender = appMailSender;
    }

    public void sendPasswordEmail(String email, String subject, String password) {
        appMailSender.send(email, subject, password);
    }

    public void sendEmail(String email, String subject, String body) {
        appMailSender.send(email, subject, body);
    }

    public List<String> getSentEmails() {
        return appMailSender.sentEmails();
    }
}
