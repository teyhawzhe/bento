package com.lovius.bento.service;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    private final List<String> sentEmails = new ArrayList<>();

    public void sendPasswordEmail(String email, String subject, String password) {
        sentEmails.add(email + "|" + subject + "|" + password);
    }

    public void sendEmail(String email, String subject, String body) {
        sentEmails.add(email + "|" + subject + "|" + body);
    }

    public List<String> getSentEmails() {
        return List.copyOf(sentEmails);
    }
}
