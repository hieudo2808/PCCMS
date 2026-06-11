package com.astral.express.pccms.appointment.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

public final class ClinicDateTime {

    public static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private ClinicDateTime() {
        // Prevent instantiation
    }

    public static OffsetDateTime toOffsetDateTime(LocalDate date, LocalTime time) {
        return date.atTime(time).atZone(ZONE).toOffsetDateTime();
    }

    public static LocalDate today() {
        return LocalDate.now(ZONE);
    }

    public static LocalTime nowTime() {
        return LocalTime.now(ZONE);
    }

    public static OffsetDateTime now() {
        return OffsetDateTime.now(ZONE);
    }

    public static OffsetDateTime startOfDay(LocalDate date) {
        return date.atStartOfDay(ZONE).toOffsetDateTime();
    }

    public static OffsetDateTime endOfDay(LocalDate date) {
        return date.plusDays(1).atStartOfDay(ZONE).toOffsetDateTime();
    }
}
