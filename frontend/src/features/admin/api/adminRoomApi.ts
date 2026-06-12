import axiosClient from "~/shared/api/axiosClient";
import { normalizePage, toSpringPage } from "~/shared/api/pageUtils";
import type {
    CreateRoomRequest,
    CreateRoomTypeRequest,
    RoomResponse,
    RoomStatus,
    RoomTypeResponse,
    UpdateRoomRequest,
    UpdateRoomTypeRequest,
} from "~/types/catalog";

export const adminRoomApi = {
    listRooms: async (params?: { page?: number; size?: number; roomTypeId?: string; statusCode?: RoomStatus | "" }) => {
        const raw = await axiosClient.get("/v1/admin/rooms", {
            params: {
                ...params,
                statusCode: params?.statusCode || undefined,
                roomTypeId: params?.roomTypeId || undefined,
                page: params?.page != null ? toSpringPage(params.page) : undefined,
            },
        });
        return normalizePage<RoomResponse>(raw);
    },

    getRoom: (id: string) => axiosClient.get<unknown, RoomResponse>(`/v1/admin/rooms/${id}`),

    createRoom: (data: CreateRoomRequest) =>
        axiosClient.post<CreateRoomRequest, RoomResponse>("/v1/admin/rooms", data),

    updateRoom: (id: string, data: UpdateRoomRequest) =>
        axiosClient.put<UpdateRoomRequest, RoomResponse>(`/v1/admin/rooms/${id}`, data),

    updateRoomStatus: (id: string, statusCode: RoomStatus) =>
        axiosClient.patch<{ statusCode: RoomStatus }, RoomResponse>(`/v1/admin/rooms/${id}/status`, { statusCode }),

    deleteRoom: (id: string) => axiosClient.delete(`/v1/admin/rooms/${id}`),

    listRoomTypes: (activeOnly = false) =>
        axiosClient.get<unknown, RoomTypeResponse[]>("/v1/admin/room-types", { params: { activeOnly } }),

    getRoomType: (id: string) => axiosClient.get<unknown, RoomTypeResponse>(`/v1/admin/room-types/${id}`),

    createRoomType: (data: CreateRoomTypeRequest) =>
        axiosClient.post<CreateRoomTypeRequest, RoomTypeResponse>("/v1/admin/room-types", data),

    updateRoomType: (id: string, data: UpdateRoomTypeRequest) =>
        axiosClient.put<UpdateRoomTypeRequest, RoomTypeResponse>(`/v1/admin/room-types/${id}`, data),

    deleteRoomType: (id: string) => axiosClient.delete(`/v1/admin/room-types/${id}`),
};
