package com.astral.express.pccms.catalog.security;

public final class ServiceCatalogPermissions {

    private ServiceCatalogPermissions() {
    }

    public static final String SERVICE_MANAGE = "hasAuthority('SERVICE_MANAGE')";
    public static final String SERVICE_READ =
            "hasAuthority('SERVICE_MANAGE') or hasAuthority('APPOINTMENT_CREATE') "
                    + "or hasAuthority('APPOINTMENT_RECEIVE') or hasAuthority('GROOMING_CREATE') "
                    + "or hasAuthority('BOARDING_CREATE') or hasAuthority('INVOICE_MANAGE')";
}
