package com.lovius.bento.service;

import java.util.ArrayList;
import java.util.List;

public class MockMailSender implements AppMailSender {
    private final List<String> sentEmails = new ArrayList<>();

    @Override
    public void send(String email, String subject, String body) {
        sentEmails.add(email + "|" + subject + "|" + body);
    }

    @Override
    public List<String> sentEmails() {
        return List.copyOf(sentEmails);
    }
}
