package ru.astondevs.notification;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import ru.astondevs.notification.event.UserEvent;
import ru.astondevs.notification.event.UserEventType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = {"user-events"})
class NotificationServiceTest {

    @Autowired
    private KafkaTemplate<String, UserEvent> kafkaTemplate;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    private GreenMail greenMail;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        greenMail = new GreenMail(new ServerSetup(2525, "localhost", "smtp"));
        greenMail.start();
        // Проверка, что GreenMail запущен
        System.out.println("GreenMail started on port 2525");
    }

    @AfterEach
    void tearDown() {
        if (greenMail != null) {
            greenMail.stop();
            System.out.println("GreenMail stopped");
        }
    }

    @Test
    void handleUserEvent_created_sendsEmail() throws Exception {
        UserEvent event = UserEvent.builder()
                .type(UserEventType.CREATED)
                .email("test@example.com")
                .build();

        kafkaTemplate.send("user-events", event);

        // Wait for email to be processed
        Thread.sleep(1000);

        var messages = greenMail.getReceivedMessages();
        assertEquals(1, messages.length, "Expected one email");
        assertEquals("test@example.com", messages[0].getAllRecipients()[0].toString());
        assertEquals("Account Created", messages[0].getSubject());
        assertEquals("Здравствуйте! Ваш аккаунт на сайте был успешно создан.",
                greenMail.getReceivedMessages()[0].getContent().toString().trim());
    }

    @Test
    void handleUserEvent_deleted_sendsEmail() throws Exception {
        UserEvent event = UserEvent.builder()
                .type(UserEventType.DELETED)
                .email("test@example.com")
                .build();

        kafkaTemplate.send("user-events", event);

        // Wait for email to be processed
        Thread.sleep(1000);

        var messages = greenMail.getReceivedMessages();
        assertEquals(1, messages.length, "Expected one email");
        assertEquals("test@example.com", messages[0].getAllRecipients()[0].toString());
        assertEquals("Account Deleted", messages[0].getSubject());
        assertEquals("Здравствуйте! Ваш аккаунт был удалён.",
                greenMail.getReceivedMessages()[0].getContent().toString().trim());
    }

    @Test
    void sendEmailApi_sendsEmail() throws Exception {
        String json = """
                {
                    "email": "test@example.com",
                    "subject": "Test Subject",
                    "message": "Test Message"
                }
                """;

        mockMvc.perform(post("/api/notifications/send-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(content().string("Email sent successfully"));

        // Wait for email to be processed
        Thread.sleep(1000);

        var messages = greenMail.getReceivedMessages();
        assertEquals(1, messages.length, "Expected one email");
        assertEquals("test@example.com", messages[0].getAllRecipients()[0].toString());
        assertEquals("Test Subject", messages[0].getSubject());
        assertEquals("Test Message", messages[0].getContent().toString().trim());
    }

    @Test
    void sendEmailApi_invalidEmail_returnsBadRequest() throws Exception {
        String json = """
                {
                    "email": "invalid-email",
                    "subject": "Test Subject",
                    "message": "Test Message"
                }
                """;

        mockMvc.perform(post("/api/notifications/send-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }
}