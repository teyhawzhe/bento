package com.lovius.bento.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

class SmtpMailSenderTest {
    @Test
    void sendUsesConfiguredFromAndMessageFields() {
        JavaMailSender javaMailSender = Mockito.mock(JavaMailSender.class);
        SmtpMailSender smtpMailSender = new SmtpMailSender(javaMailSender, "no-reply@company.local");

        smtpMailSender.send("alice@company.local", "測試主旨", "測試內容");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(javaMailSender).send(captor.capture());
        SimpleMailMessage message = captor.getValue();
        assertEquals("no-reply@company.local", message.getFrom());
        assertEquals("測試主旨", message.getSubject());
        assertEquals("測試內容", message.getText());
        assertEquals("alice@company.local", message.getTo()[0]);
    }
}
