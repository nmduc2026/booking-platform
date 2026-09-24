package com.ndz.venue_service.config;

import java.util.UUID;

/**
 * Fixed demo IDs shared conceptually with auth-service seed.
 */
public final class DemoSeedIds {

    public static final UUID ADMIN_ID = UUID.fromString("00000000-0000-4000-8000-000000000001");
    public static final UUID SHOP_ID = UUID.fromString("00000000-0000-4000-8000-000000000101");
    public static final UUID ROOM_RESOURCE_ID = UUID.fromString("00000000-0000-4000-8000-000000000201");
    public static final UUID STAFF_RESOURCE_ID = UUID.fromString("00000000-0000-4000-8000-000000000202");

    private DemoSeedIds() {
    }
}
