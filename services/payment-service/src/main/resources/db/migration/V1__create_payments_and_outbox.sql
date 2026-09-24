CREATE TABLE payments (
    id                        UUID PRIMARY KEY,
    booking_id                UUID           NOT NULL,
    stripe_payment_intent_id  VARCHAR(255)   NOT NULL,
    amount                    DECIMAL(10, 2) NOT NULL,
    currency                  VARCHAR(10)    NOT NULL,
    status                    VARCHAR(20)    NOT NULL,
    created_at                TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at                TIMESTAMP
);

CREATE UNIQUE INDEX uq_payments_stripe_payment_intent_id ON payments (stripe_payment_intent_id);
CREATE INDEX idx_payments_booking_id ON payments (booking_id);

CREATE TABLE outbox_events (
    id            UUID PRIMARY KEY,
    aggregate_id  UUID         NOT NULL,
    event_type    VARCHAR(50)  NOT NULL,
    payload       JSONB        NOT NULL,
    status        VARCHAR(20)  NOT NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    sent_at       TIMESTAMP
);

CREATE INDEX idx_outbox_events_status_created_at ON outbox_events (status, created_at);
