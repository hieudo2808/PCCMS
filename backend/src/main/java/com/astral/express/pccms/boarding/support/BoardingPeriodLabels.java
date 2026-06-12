package com.astral.express.pccms.boarding.support;

public final class BoardingPeriodLabels {

    private BoardingPeriodLabels() {}

    public static String toPeriodLabel(String periodCode) {
        return switch (periodCode) {
            case "MORNING" -> "Sáng";
            case "NOON" -> "Trưa";
            case "AFTERNOON" -> "Chiều";
            default -> periodCode;
        };
    }
}
