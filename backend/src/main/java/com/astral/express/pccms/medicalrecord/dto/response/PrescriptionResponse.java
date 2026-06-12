package com.astral.express.pccms.medicalrecord.dto.response;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record PrescriptionResponse(
        UUID id,
        String prescriptionCode,
        UUID medicalRecordId,
        UUID vetId,
        String note,
        OffsetDateTime issuedAt,
        List<PrescriptionItemResponse> items
) {
}
