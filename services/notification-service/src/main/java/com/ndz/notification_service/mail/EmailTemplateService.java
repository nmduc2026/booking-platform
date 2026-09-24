package com.ndz.notification_service.mail;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class EmailTemplateService {

    public String confirmSubject(String bookingId) {
        return "Booking confirmed — #" + shortId(bookingId);
    }

    public String cancelSubject(String bookingId) {
        return "Booking cancelled — #" + shortId(bookingId);
    }

    public String confirmBody(String bookingId, String shopId, String amount) {
        return """
                <html><body>
                <h2>Your booking is confirmed</h2>
                <p>Thank you for booking with us.</p>
                <ul>
                  <li><strong>Booking ID:</strong> %s</li>
                  <li><strong>Shop ID:</strong> %s</li>
                  <li><strong>Amount:</strong> %s</li>
                </ul>
                <p>We look forward to seeing you.</p>
                </body></html>
                """.formatted(bookingId, shopId, amount);
    }

    public String cancelBody(String bookingId, String shopId, String amount) {
        return """
                <html><body>
                <h2>Your booking was cancelled</h2>
                <p>The following booking is no longer active.</p>
                <ul>
                  <li><strong>Booking ID:</strong> %s</li>
                  <li><strong>Shop ID:</strong> %s</li>
                  <li><strong>Amount:</strong> %s</li>
                </ul>
                <p>If this was unexpected, please contact the shop.</p>
                </body></html>
                """.formatted(bookingId, shopId, amount);
    }

    public static String formatAmount(BigDecimal amount) {
        if (amount == null) {
            return "n/a";
        }
        return amount.toPlainString();
    }

    private static String shortId(String id) {
        if (id == null || id.length() < 8) {
            return id == null ? "" : id;
        }
        return id.substring(0, 8);
    }
}
