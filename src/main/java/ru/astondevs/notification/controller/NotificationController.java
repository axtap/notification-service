package ru.astondevs.notificationservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.astondevs.notificationservice.dto.EmailRequest;
import ru.astondevs.notificationservice.service.EmailService;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final EmailService emailService;

    @PostMapping("/send-email")
    public String sendEmail(@RequestBody EmailRequest request) {
        emailService.sendEmail(request.getEmail(), request.getSubject(), request.getMessage());
        return "Email sent successfully";
    }

}
