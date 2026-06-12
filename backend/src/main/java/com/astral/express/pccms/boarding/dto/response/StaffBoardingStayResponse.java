package com.astral.express.pccms.boarding.dto.response;

import java.util.UUID;

public record StaffBoardingStayResponse(
        UUID sessionId,
        UUID petId,
        String petName,
        String roomLabel,
        int currentDay,
        int totalDays,
        String todayLogSummary
) {}
