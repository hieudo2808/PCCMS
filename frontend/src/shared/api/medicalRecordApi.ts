import axiosClient from "./axiosClient";
import type {
    MedicalRecordResponse,
    UpdateMedicalRecordRequest,
    FinalizeMedicalRecordRequest,
    MedicalRecordOwnerResponse
} from "~/types/medicalRecord";

export interface PrescriptionItemRequest {
    medicineId: string;
    dosage?: string;
    quantity: number;
    instruction: string;
}

export interface CreatePrescriptionRequest {
    note?: string;
    items: PrescriptionItemRequest[];
}

export interface PrescriptionItemResponse {
    id: string;
    medicineId: string;
    medicineName?: string;
    medicineUnit?: string;
    dosage: string;
    quantity: number;
    instruction?: string;
    unitPriceVnd: number;
}

export interface PrescriptionResponse {
    id: string;
    prescriptionCode: string;
    medicalRecordId: string;
    vetId: string;
    note?: string;
    issuedAt: string;
    items: PrescriptionItemResponse[];
}

export const medicalRecordApi = {
    getMedicalRecordById: (id: string): Promise<MedicalRecordResponse> => {
        return axiosClient.get(`/v1/medical-records/${id}`);
    },

    getMedicalRecords: (vetId?: string): Promise<MedicalRecordResponse[]> => {
        return axiosClient.get("/v1/medical-records", { params: { vetId } });
    },


    getOrCreateMedicalRecordByAppointmentId: (appointmentId: string): Promise<MedicalRecordResponse> => {
        return axiosClient.get(`/v1/medical-records/appointment/${appointmentId}`);
    },

    updateMedicalRecord: (
        id: string,
        data: UpdateMedicalRecordRequest
    ): Promise<MedicalRecordResponse> => {
        return axiosClient.put(`/v1/medical-records/${id}`, data);
    },

    finalizeMedicalRecord: (
        id: string,
        data: FinalizeMedicalRecordRequest
    ): Promise<MedicalRecordResponse> => {
        return axiosClient.patch(`/v1/medical-records/${id}/finalize`, data);
    },

    getOwnerMedicalRecords: (petId: string): Promise<MedicalRecordOwnerResponse[]> => {
        return axiosClient.get(`/v1/owner/pets/${petId}/medical-records`);
    },

    createPrescription: (
        recordId: string,
        data: CreatePrescriptionRequest
    ): Promise<PrescriptionResponse> => {
        return axiosClient.post(`/v1/medical-records/${recordId}/prescriptions`, data);
    },

    listPrescriptions: (recordId: string): Promise<PrescriptionResponse[]> => {
        return axiosClient.get(`/v1/medical-records/${recordId}/prescriptions`);
    },
};
