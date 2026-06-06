import axiosClient from '~/shared/api/axiosClient';
import { normalizePage, toSpringPage } from '~/shared/api/pageUtils';
import type { PageResponse } from '~/types/api';
import type { MedicineResponse, CreateMedicineRequest, UpdateMedicineRequest, AddStockRequest } from '~/types/medicine';

export const medicineApi = {
  getMedicines: async (page = 1, size = 10, categoryId?: string) => {
    const raw = await axiosClient.get('/v1/medicines', {
      params: { page: toSpringPage(page), size, ...(categoryId ? { categoryId } : {}) },
    });
    return normalizePage<MedicineResponse>(raw);
  },

  getMedicineById: (id: string) => {
    return axiosClient.get<MedicineResponse>(`/v1/medicines/${id}`);
  },

  createMedicine: (data: CreateMedicineRequest) => {
    return axiosClient.post<CreateMedicineRequest, MedicineResponse>('/v1/medicines', data);
  },

  updateMedicine: (id: string, data: UpdateMedicineRequest) => {
    return axiosClient.put<UpdateMedicineRequest, MedicineResponse>(`/v1/medicines/${id}`, data);
  },

  addStock: (id: string, data: AddStockRequest) => {
    return axiosClient.patch<AddStockRequest, MedicineResponse>(`/v1/medicines/${id}/stock`, data);
  },

  deleteMedicine: (id: string) => {
    return axiosClient.delete(`/v1/medicines/${id}`);
  },
};
