package com.astral.express.pccms.schedule.dto.response;

import java.util.UUID;

public record ExamRoomOptionResponse(
        UUID id,
        String roomCode,
        String name
) {
}
