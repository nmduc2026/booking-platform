CREATE TABLE bookings (
    id          UUID PRIMARY KEY,
    user_id     UUID           NOT NULL,
    shop_id     UUID           NOT NULL,
    slot_id     UUID           NOT NULL,
    status      VARCHAR(20)    NOT NULL,
    amount      DECIMAL(10, 2) NOT NULL,
    expires_at  TIMESTAMP      NOT NULL,
    created_at  TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP
);

CREATE INDEX idx_bookings_user_id ON bookings (user_id);
CREATE INDEX idx_bookings_slot_id ON bookings (slot_id);
CREATE INDEX idx_bookings_status_expires_at ON bookings (status, expires_at);
