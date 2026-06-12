package com.astral.express.pccms.medicalrecord.event;

import java.util.UUID;

/**
 * Event published when a medical record transitions from DRAFT to FINALIZED.
 * Consumers can use this to trigger downstream actions (notifications, billing, etc.)
 * without creating tight coupling to the medical record module.
 */
public record MedicalRecordFinalizedEvent(
        UUID recordId,
        UUID petId,
        UUID vetId
) {
}
