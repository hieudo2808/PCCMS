import axiosClient from "~/shared/api/axiosClient";
import type { PageResponse } from "~/types/api";
import type {
    MedicineResponse,
    CreateMedicineRequest,
    UpdateMedicineRequest,
    AddStockRequest,
} from "~/types/medicine";

export const medicineApi = {
    getMedicines: (page = 1, size = 10, search?: string) => {
        return axiosClient.get<any, PageResponse<MedicineResponse>>("/v1/medicines", {
            params: { page, size, ...(search ? { search } : {}) },
        });
    },

    getMedicineById: (id: string) => {
        return axiosClient.get<any, MedicineResponse>(`/v1/medicines/${id}`);
    },

    createMedicine: (data: CreateMedicineRequest) => {
        return axiosClient.post<any, MedicineResponse>("/v1/medicines", data);
    },

    updateMedicine: (id: string, data: UpdateMedicineRequest) => {
        return axiosClient.put<any, MedicineResponse>(`/v1/medicines/${id}`, data);
    },

    addStock: (id: string, data: AddStockRequest) => {
        return axiosClient.patch<any, MedicineResponse>(`/v1/medicines/${id}/stock`, data);
    },

    deleteMedicine: (id: string) => {
        return axiosClient.delete<any, void>(`/v1/medicines/${id}`);
    },
};
