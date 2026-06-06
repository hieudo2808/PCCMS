package com.astral.express.pccms.appointment.dto.response;

public record AvailabilitySummaryResponse(
        int totalExamRooms,
        int vetsOnDuty,
        int totalSlots,
        int availableSlots,
        Integer freeRoomsForSlot,
        Integer freeVetsForSlot
) {}
