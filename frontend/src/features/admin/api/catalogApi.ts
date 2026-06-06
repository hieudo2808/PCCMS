import axiosClient from '~/shared/api/axiosClient';
import { normalizePage, toSpringPage } from '~/shared/api/pageUtils';
import type { PageResponse } from '~/types/api';
import type {
  CreateMedicineCategoryRequest,
  CreateRoomRequest,
  CreateRoomTypeRequest,
  CreateServiceCatalogRequest,
  MedicineCategoryResponse,
  RoomResponse,
  RoomTypeResponse,
  ServiceCatalogResponse,
  ServiceCategory,
  UpdateMedicineCategoryRequest,
  UpdateRoomRequest,
  UpdateRoomTypeRequest,
  UpdateServiceCatalogRequest,
} from '~/types/catalog';

export const catalogApi = {
  // Service catalog
  listServices: async (params?: { page?: number; size?: number; categoryCode?: ServiceCategory }) => {
    const raw = await axiosClient.get('/v1/catalog/services', {
      params: {
        ...params,
        page: params?.page != null ? toSpringPage(params.page) : undefined,
      },
    });
    return normalizePage<ServiceCatalogResponse>(raw);
  },

  listServiceCategories: () => {
    return axiosClient.get<ServiceCategory[]>('/v1/catalog/services/categories');
  },

  createService: (data: CreateServiceCatalogRequest) => {
    return axiosClient.post<CreateServiceCatalogRequest, ServiceCatalogResponse>('/v1/catalog/services', data);
  },

  updateService: (id: string, data: UpdateServiceCatalogRequest) => {
    return axiosClient.put<UpdateServiceCatalogRequest, ServiceCatalogResponse>(`/v1/catalog/services/${id}`, data);
  },

  deleteService: (id: string) => {
    return axiosClient.delete(`/v1/catalog/services/${id}`);
  },

  // Medicine categories
  listMedicineCategories: (activeOnly = true) => {
    return axiosClient.get<MedicineCategoryResponse[]>('/v1/catalog/medicine-categories', { params: { activeOnly } });
  },

  createMedicineCategory: (data: CreateMedicineCategoryRequest) => {
    return axiosClient.post<CreateMedicineCategoryRequest, MedicineCategoryResponse>('/v1/catalog/medicine-categories', data);
  },

  updateMedicineCategory: (id: string, data: UpdateMedicineCategoryRequest) => {
    return axiosClient.put<UpdateMedicineCategoryRequest, MedicineCategoryResponse>(`/v1/catalog/medicine-categories/${id}`, data);
  },

  deleteMedicineCategory: (id: string) => {
    return axiosClient.delete(`/v1/catalog/medicine-categories/${id}`);
  },

  // Rooms
  listRooms: async (params?: { page?: number; size?: number; roomTypeId?: string; statusCode?: string }) => {
    const raw = await axiosClient.get('/v1/catalog/rooms', {
      params: {
        ...params,
        page: params?.page != null ? toSpringPage(params.page) : undefined,
      },
    });
    return normalizePage<RoomResponse>(raw);
  },

  createRoom: (data: CreateRoomRequest) => {
    return axiosClient.post<CreateRoomRequest, RoomResponse>('/v1/catalog/rooms', data);
  },

  updateRoom: (id: string, data: UpdateRoomRequest) => {
    return axiosClient.put<UpdateRoomRequest, RoomResponse>(`/v1/catalog/rooms/${id}`, data);
  },

  deleteRoom: (id: string) => {
    return axiosClient.delete(`/v1/catalog/rooms/${id}`);
  },

  // Room types
  listRoomTypes: (activeOnly = true) => {
    return axiosClient.get<RoomTypeResponse[]>('/v1/catalog/room-types', { params: { activeOnly } });
  },

  createRoomType: (data: CreateRoomTypeRequest) => {
    return axiosClient.post<CreateRoomTypeRequest, RoomTypeResponse>('/v1/catalog/room-types', data);
  },

  updateRoomType: (id: string, data: UpdateRoomTypeRequest) => {
    return axiosClient.put<UpdateRoomTypeRequest, RoomTypeResponse>(`/v1/catalog/room-types/${id}`, data);
  },

  deleteRoomType: (id: string) => {
    return axiosClient.delete(`/v1/catalog/room-types/${id}`);
  },
};

/** @deprecated Use catalogApi.listMedicineCategories */
export const medicineCategoryApi = {
  listCategories: () => catalogApi.listMedicineCategories(true),
};
