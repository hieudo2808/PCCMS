export type RecordStatus = "DRAFT" | "FINALIZED" | "CANCELLED";
export type AlertSeverity = "INFO" | "WARNING" | "CRITICAL";

export interface HealthAlertResponse {
    id: string;
    petId: string;
    medicalRecordId: string;
    severity: AlertSeverity;
    message: string;
    createdAt: string;
}

export interface MedicalRecordResponse {
    id: string;
    recordCode: string;
    appointmentId: string;
    petId: string;
    vetId: string;
    recordStatus: RecordStatus;
    temperatureC: number;
    heartRateBpm: number;
    respiratoryRateBpm: number;
    weightKg: number;
    bloodPressure: string;
    spo2Percent: number;
    mucousMembraneColor: string;
    capillaryRefillSeconds: number;
    preliminaryDiagnosis: string;
    finalDiagnosis: string;
    treatmentNote: string;
    followUpAt: string;
    lockedAt: string;
    createdAt: string;
    updatedAt: string;
}

export interface UpdateMedicalRecordRequest {
    temperatureC?: number;
    heartRateBpm?: number;
    respiratoryRateBpm?: number;
    weightKg?: number;
    bloodPressure?: string;
    spo2Percent?: number;
    mucousMembraneColor?: string;
    capillaryRefillSeconds?: number;
    preliminaryDiagnosis?: string;
    treatmentNote?: string;
}

export interface FinalizeMedicalRecordRequest {
    finalDiagnosis: string;
    treatmentNote?: string;
    followUpAt?: string;
}
