package com.ndz.auth_service.config;

import java.util.UUID;

/**
 * Fixed demo IDs shared conceptually with venue-service seed.
 */
public final class DemoSeedIds {

    public static final UUID ADMIN_ID = UUID.fromString("00000000-0000-4000-8000-000000000001");
    public static final UUID MANAGER_ID = UUID.fromString("00000000-0000-4000-8000-000000000002");
    public static final UUID USER_ID = UUID.fromString("00000000-0000-4000-8000-000000000003");
    public static final UUID SHOP_ID = UUID.fromString("00000000-0000-4000-8000-000000000101");

    private DemoSeedIds() {
    }
}
