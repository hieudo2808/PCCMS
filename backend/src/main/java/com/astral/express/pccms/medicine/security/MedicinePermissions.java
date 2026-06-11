package com.astral.express.pccms.medicine.security;

public final class MedicinePermissions {

    private MedicinePermissions() {
    }

    public static final String MEDICINE_MANAGE = "hasAuthority('MEDICINE_MANAGE')";
    public static final String MEDICINE_READ =
            "hasAuthority('MEDICINE_MANAGE') or hasAuthority('PRESCRIPTION_CREATE')";
}
