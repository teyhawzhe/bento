package com.lovius.bento.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

public class SmtpMailSender implements AppMailSender {
    private final JavaMailSender mailSender;
    private final String from;

    public SmtpMailSender(JavaMailSender mailSender, String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    @Override
    public void send(String email, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setFrom(from);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }
}
