package com.hotel.channelmanager.common.tenant;

public final class TenantContext {

    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContext() {}

    public static void setOrganizationId(String organizationId) {
        CURRENT_TENANT.set(organizationId);
    }

    public static String getOrganizationId() {
        return CURRENT_TENANT.get();
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }
}

