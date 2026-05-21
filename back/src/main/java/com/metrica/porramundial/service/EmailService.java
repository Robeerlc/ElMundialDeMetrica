package com.metrica.porramundial.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String senderEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendActivationEmail(String toEmail, String activationToken) {
        String activationLink = "http://localhost:4200/activar?token=" + activationToken;
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(senderEmail);
        message.setTo(toEmail);
        message.setSubject("Métrica - Activa tu cuenta para la Porra del Mundial");
        message.setText("¡Hola!\n\n" +
                "Te has registrado en la Porra Interna de Métrica.\n" +
                "Para confirmar que este es tu correo corporativo y empezar a jugar, haz clic en el siguiente enlace:\n\n" +
                activationLink + "\n\n" +
                "¡Mucha suerte con los pronósticos!");
        mailSender.send(message);
    }
}