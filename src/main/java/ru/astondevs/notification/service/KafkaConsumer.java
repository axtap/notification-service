package ru.astondevs.notificationservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import ru.astondevs.notificationservice.event.UserEvent;
import ru.astondevs.notificationservice.event.UserEventType;

@Service
@RequiredArgsConstructor
public class KafkaConsumer {
    private final EmailService emailService;

    @KafkaListener(topics = "user-events", groupId = "notification-group")
    public void handleUserEvent(UserEvent userEvent) {
        String message;
        if (userEvent.getType() == UserEventType.CREATED) {
            message = "Здравствуйте! Ваш аккаунт на сайте был успешно создан.";
        } else {
            message = "Здравствуйте! Ваш аккаунт был удалён.";
        }
        emailService.sendEmail(userEvent.getEmail(), "Уведомление о вашем аккаунте", message);
    }
}
