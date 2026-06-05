import axiosClient from './axiosClient';
import type { 
  MedicalRecordResponse, 
  UpdateMedicalRecordRequest, 
  FinalizeMedicalRecordRequest 
} from '~/types/medicalRecord';

export interface PrescriptionItemRequest {
  medicineId: string;
  quantity: number;
  dosageInstruction: string;
}

export interface CreatePrescriptionRequest {
  items: PrescriptionItemRequest[];
}

export interface PrescriptionResponse {
  id: string;
  medicalRecordId: string;
  items: any[]; // define later if needed
}

export const medicalRecordApi = {
  getMedicalRecordById: (id: string): Promise<MedicalRecordResponse> => {
    return axiosClient.get(`/api/v1/medical-records/${id}`);
  },

  updateMedicalRecord: (id: string, data: UpdateMedicalRecordRequest): Promise<MedicalRecordResponse> => {
    return axiosClient.put(`/api/v1/medical-records/${id}`, data);
  },

  finalizeMedicalRecord: (id: string, data: FinalizeMedicalRecordRequest): Promise<MedicalRecordResponse> => {
    return axiosClient.patch(`/api/v1/medical-records/${id}/finalize`, data);
  },
  
  createPrescription: (recordId: string, data: CreatePrescriptionRequest): Promise<PrescriptionResponse> => {
    return axiosClient.post(`/api/v1/medical-records/${recordId}/prescriptions`, data);
  }
};
