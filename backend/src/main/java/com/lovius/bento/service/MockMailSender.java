package com.lovius.bento.service;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MockMailSender implements AppMailSender {
    private static final Logger logger = LoggerFactory.getLogger(MockMailSender.class);
    private static final String LOG_PREFIX = "MOCK_EMAIL";
    private final List<String> sentEmails = new ArrayList<>();

    @Override
    public void send(String email, String subject, String body) {
        logger.info(
                "{} recipient={} subject={} body=\n{}",
                LOG_PREFIX,
                email,
                subject,
                body);
        sentEmails.add(email + "|" + subject + "|" + body);
    }

    @Override
    public List<String> sentEmails() {
        return List.copyOf(sentEmails);
    }
}
