import axiosClient from '~/shared/api/axiosClient';
import type { PetResponse, PetRequest } from '~/types/pet';
import type { PageResponse } from '~/types/api';

/** Backend bọc PageResponse trong ApiResponse → sau unwrap còn lớp data.content */
function normalizePage<T>(raw: PageResponse<T> & { data?: PageResponse<T> }): PageResponse<T> {
  if (raw && 'content' in raw && Array.isArray(raw.content)) {
    return raw;
  }
  if (raw?.data && 'content' in raw.data) {
    return raw.data;
  }
  return raw;
}

export const petApi = {
  getPets: async (params?: {
    page?: number;
    size?: number;
    isActive?: boolean;
    ownerId?: string;
  }): Promise<PageResponse<PetResponse>> => {
    const raw = await axiosClient.get<PageResponse<PetResponse> & { data?: PageResponse<PetResponse> }>('/v1/pets', { params });
    return normalizePage(raw);
  },

  getPetById: (id: string): Promise<PetResponse> => {
    return axiosClient.get(`/v1/pets/${id}`);
  },

  createPet: (data: PetRequest): Promise<PetResponse> => {
    return axiosClient.post('/v1/pets', data);
  },

  updatePet: (id: string, data: PetRequest): Promise<PetResponse> => {
    return axiosClient.put(`/v1/pets/${id}`, data);
  },
  
  deletePet: (id: string): Promise<void> => {
    return axiosClient.delete(`/v1/pets/${id}`);
  }
};
