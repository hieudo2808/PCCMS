package com.astral.express.pccms.appointment.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record QueueEntryResponse(
        Integer queueNumber,
        UUID appointmentId,
        UUID petId,
        String petName,
        String ownerName,
        OffsetDateTime checkedInAt,
        String symptomText
) {}
