package com.astral.express.pccms.catalog.security;

public final class CatalogPermissions {

    private CatalogPermissions() {
    }

    public static final String MEDICINE_MANAGE = "hasAuthority('MEDICINE_MANAGE')";
    public static final String MEDICINE_READ =
            "hasAuthority('MEDICINE_MANAGE') or hasAuthority('PRESCRIPTION_CREATE')";

    public static final String SERVICE_MANAGE = "hasAuthority('SERVICE_MANAGE')";
    public static final String SERVICE_READ =
            "hasAuthority('SERVICE_MANAGE') or hasAuthority('APPOINTMENT_CREATE') "
                    + "or hasAuthority('APPOINTMENT_RECEIVE') or hasAuthority('GROOMING_CREATE') "
                    + "or hasAuthority('BOARDING_CREATE') or hasAuthority('INVOICE_MANAGE')";

    public static final String ROOM_MANAGE = "hasAuthority('ROOM_MANAGE')";
    public static final String ROOM_READ =
            "hasAuthority('ROOM_MANAGE') or hasAuthority('BOARDING_READ') "
                    + "or hasAuthority('BOARDING_CREATE') or hasAuthority('BOARDING_UPDATE')";
}
