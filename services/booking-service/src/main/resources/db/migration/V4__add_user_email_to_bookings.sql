ALTER TABLE booking_schema.bookings
    ADD COLUMN IF NOT EXISTS user_email VARCHAR(255);
