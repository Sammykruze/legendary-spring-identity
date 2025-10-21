package com.legendaryUser.legendary.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.UUID;

@Service
public class EmailService {
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Value("${app.email.from}")
    private String fromEmail;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private TemplateEngine templateEngine;

    @Async
    public void sendEmailVerification(String to, String token, String name) {
        String verificationUrl = frontendUrl + "/verify-email?token=" + token;

        Context context = new Context();
        context.setVariable("name", name);
        context.setVariable("verificationUrl", verificationUrl);

        String subject = "Verify Your Email Address";
        String htmlContent = templateEngine.process("email-verification", context);

        sendEmail(to, subject, htmlContent);
    }

    @Async
    public void sendOtpEmail(String to, String otp, String name) {
        Context context = new Context();
        context.setVariable("name", name);
        context.setVariable("otp", otp);

        String subject = "Your Login OTP Code";
        String htmlContent = templateEngine.process("email-otp", context);

        sendEmail(to, subject, htmlContent);
    }

    private void sendEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            logger.info("Email sent successfully to: {}", to);
        } catch (MessagingException e) {
            logger.error("Failed to send email to: {}, error: {}", to, e.getMessage());
            throw new RuntimeException("Failed to send email", e);
        }
    }

    public String generateEmailVerificationToken() {
        return UUID.randomUUID().toString();
    }
}

