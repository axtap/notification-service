package ru.astondevs.notificationservice.dto;

import lombok.Data;

@Data
public class EmailRequest {
    private String email;
    private String subject;
    private String message;
}
