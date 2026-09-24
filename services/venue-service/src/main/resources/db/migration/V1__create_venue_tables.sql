CREATE TABLE shops (
    id                   UUID PRIMARY KEY,
    name                 VARCHAR(255) NOT NULL,
    address              VARCHAR(500),
    description          TEXT,
    created_by_admin_id  UUID         NOT NULL,
    status               VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at           TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP
);

CREATE TABLE resources (
    id          UUID PRIMARY KEY,
    shop_id     UUID         NOT NULL REFERENCES shops (id),
    name        VARCHAR(255) NOT NULL,
    type        VARCHAR(20)  NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_resources_shop_id ON resources (shop_id);

CREATE TABLE time_slots (
    id           UUID PRIMARY KEY,
    shop_id      UUID           NOT NULL REFERENCES shops (id),
    resource_id  UUID           NOT NULL REFERENCES resources (id),
    start_time   TIMESTAMP      NOT NULL,
    end_time     TIMESTAMP      NOT NULL,
    price        DECIMAL(10, 2) NOT NULL,
    status       VARCHAR(20)    NOT NULL DEFAULT 'AVAILABLE',
    created_at   TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP
);

CREATE UNIQUE INDEX uk_time_slots_resource_start ON time_slots (resource_id, start_time);
CREATE INDEX idx_time_slots_shop_start_status ON time_slots (shop_id, start_time, status);
