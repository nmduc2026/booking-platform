package com.ndz.booking_service.job;

import com.ndz.booking_service.service.BookingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.booking", name = "expiry-job-enabled", havingValue = "true", matchIfMissing = true)
public class PendingBookingExpiryJob {

    private static final Logger log = LoggerFactory.getLogger(PendingBookingExpiryJob.class);

    private final BookingService bookingService;

    public PendingBookingExpiryJob(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @Scheduled(fixedDelayString = "${app.booking.expiry-poll-interval-ms:30000}")
    public void expirePending() {
        int cancelled = bookingService.expireOverduePendingBookings();
        if (cancelled > 0) {
            log.info("Expired {} overdue PENDING booking(s)", cancelled);
        }
    }
}
