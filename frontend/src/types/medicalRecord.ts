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

export interface PrescriptionResponse {
    id: string;
    prescriptionCode: string;
    medicalRecordId: string;
    vetId: string;
    note: string;
    issuedAt: string;
    items: PrescriptionItemResponse[];
}

export interface PrescriptionItemResponse {
    id: string;
    medicineId: string;
    medicineName: string;
    medicineUnit: string;
    dosage: string;
    quantity: number;
    instruction: string;
    unitPriceVnd: number;
}

export interface MedicalRecordOwnerResponse {
    id: string;
    recordCode: string;
    petId: string;
    vetName: string;
    temperatureC: number;
    weightKg: number;
    heartRateBpm: number | null;
    respiratoryRateBpm: number | null;
    bloodPressure: string | null;
    spo2Percent: number | null;
    mucousMembraneColor: string | null;
    capillaryRefillSeconds: number | null;
    finalDiagnosis: string;
    treatmentNote: string;
    followUpAt: string | null;
    createdAt: string;
    prescription: PrescriptionResponse | null;
}

export interface MedicalRecordResponse {
    id: string;
    recordCode: string;
    appointmentId: string;
    petId: string;
    petName?: string;
    vetId: string;
    vetName?: string;
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

