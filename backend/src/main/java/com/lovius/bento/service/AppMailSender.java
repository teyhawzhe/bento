package com.lovius.bento.service;

import java.util.List;

public interface AppMailSender {
    void send(String email, String subject, String body);

    default List<String> sentEmails() {
        return List.of();
    }
}
