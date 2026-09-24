package com.ndz.notification_service;

import com.ndz.notification_service.config.MailProperties;
import com.ndz.notification_service.mail.EmailService;
import com.ndz.notification_service.mail.EmailTemplateService;
import com.ndz.notification_service.messaging.BookingEventHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingEventHandlerTest {

    @Mock
    EmailService emailService;

    @Mock
    EmailTemplateService templates;

    @Mock
    MailProperties mailProperties;

    @Test
    void confirmedSendsConfirmEmailToUserEmail() {
        BookingEventHandler handler = new BookingEventHandler(
                new ObjectMapper(), emailService, templates, mailProperties
        );
        when(templates.confirmSubject("b-1")).thenReturn("subj");
        when(templates.confirmBody("b-1", "s-1", "40")).thenReturn("<html/>");

        handler.handle("""
                {"eventType":"BOOKING_CONFIRMED","bookingId":"b-1","shopId":"s-1","amount":40,"userEmail":"user@test.com"}
                """);

        verify(emailService).sendHtml("user@test.com", "subj", "<html/>");
    }

    @Test
    void cancelledUsesFallbackWhenNoUserEmail() {
        BookingEventHandler handler = new BookingEventHandler(
                new ObjectMapper(), emailService, templates, mailProperties
        );
        when(mailProperties.fallbackTo()).thenReturn("fallback@test.com");
        when(templates.cancelSubject("b-2")).thenReturn("cancelled");
        when(templates.cancelBody("b-2", "s-2", "12.5")).thenReturn("<html>c</html>");

        handler.handle("""
                {"eventType":"BOOKING_CANCELLED","bookingId":"b-2","shopId":"s-2","amount":12.5}
                """);

        verify(emailService).sendHtml("fallback@test.com", "cancelled", "<html>c</html>");
    }
}
