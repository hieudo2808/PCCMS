import axiosClient from "~/shared/api/axiosClient";
import type { PageResponse } from "~/types/api";
import type {
    BoardingBookingResponse,
    BoardingStatus,
    CarePeriod,
    CareLogResponse,
    CreateBoardingBookingRequest,
    RoomAvailabilityResponse,
    RoomRequest,
    RoomResponse,
    RoomStatus,
    RoomTypeRequest,
    RoomTypeResponse,
} from "~/types/boarding";

type PageEnvelope<T> = PageResponse<T> | { success: boolean; data: PageResponse<T> };

function unwrapPage<T>(value: PageEnvelope<T>): PageResponse<T> {
    if ("data" in value && value.data && "content" in value.data) {
        return value.data;
    }
    return value as PageResponse<T>;
}

export interface CareLogFormPayload {
    sessionId: string;
    petId?: string;
    logDate: string;
    periodCode: CarePeriod;
    feedingStatus: string;
    hygieneStatus: string;
    healthNote?: string;
    staffNote?: string;
    caption?: string;
    images?: File[];
}

export const boardingApi = {
    getAvailability: (startAt: string, endAt: string): Promise<RoomAvailabilityResponse[]> =>
        axiosClient.get("/v1/boarding/availability", { params: { startAt, endAt } }),

    createBooking: (data: CreateBoardingBookingRequest): Promise<BoardingBookingResponse> =>
        axiosClient.post("/v1/boarding/bookings", data),

    getMyBookings: async (): Promise<PageResponse<BoardingBookingResponse>> => {
        const response = await axiosClient.get<any, PageEnvelope<BoardingBookingResponse>>(
            "/v1/boarding/bookings/my"
        );
        return unwrapPage(response);
    },

    getBookings: async (
        statusCode?: BoardingStatus
    ): Promise<PageResponse<BoardingBookingResponse>> => {
        const response = await axiosClient.get<any, PageEnvelope<BoardingBookingResponse>>(
            "/v1/boarding/bookings",
            {
                params: statusCode ? { statusCode } : undefined,
            }
        );
        return unwrapPage(response);
    },

    confirmBooking: (bookingId: string, roomId: string): Promise<BoardingBookingResponse> =>
        axiosClient.post(`/v1/boarding/bookings/${bookingId}/confirmations`, { roomId }),

    checkIn: (bookingId: string): Promise<BoardingBookingResponse> =>
        axiosClient.post(`/v1/boarding/bookings/${bookingId}/check-ins`),

    startStay: (bookingId: string): Promise<BoardingBookingResponse> =>
        axiosClient.post(`/v1/boarding/bookings/${bookingId}/stay-starts`),

    checkOut: (bookingId: string): Promise<BoardingBookingResponse> =>
        axiosClient.post(`/v1/boarding/bookings/${bookingId}/check-outs`),

    cancelBooking: (bookingId: string, reason: string): Promise<BoardingBookingResponse> =>
        axiosClient.post(`/v1/boarding/bookings/${bookingId}/cancellations`, { reason }),

    createCareLog: async (payload: CareLogFormPayload): Promise<CareLogResponse> => {
        const careLog = await axiosClient.post<any, CareLogResponse>("/v1/reception/boarding/care-logs", {
            sessionId: payload.sessionId,
            petId: payload.petId,
            logDate: payload.logDate,
            periodCode: payload.periodCode,
            feedingStatus: payload.feedingStatus,
            hygieneStatus: payload.hygieneStatus,
            healthNote: payload.healthNote,
            staffNote: payload.staffNote,
        });
        if (payload.images?.length) {
            await Promise.all(payload.images.map((image) => boardingApi.uploadCareLogMedia(careLog.id, image)));
        }
        return careLog;
    },

    getCareLogs: (bookingId: string): Promise<CareLogResponse[]> =>
        axiosClient.get(`/v1/boarding/bookings/${bookingId}/care-logs`),

    getReceptionCareLogs: (sessionId: string): Promise<CareLogResponse[]> =>
        axiosClient.get("/v1/reception/boarding/care-logs", { params: { sessionId } }),

    getCareLogDetail: (careLogId: string): Promise<CareLogResponse> =>
        axiosClient.get(`/v1/reception/boarding/care-logs/${careLogId}`),

    updateCareLog: (careLogId: string, payload: CareLogFormPayload): Promise<CareLogResponse> =>
        axiosClient.put(`/v1/reception/boarding/care-logs/${careLogId}`, {
            sessionId: payload.sessionId,
            petId: payload.petId,
            logDate: payload.logDate,
            periodCode: payload.periodCode,
            feedingStatus: payload.feedingStatus,
            hygieneStatus: payload.hygieneStatus,
            healthNote: payload.healthNote,
            staffNote: payload.staffNote,
        }),

    deleteCareLog: (careLogId: string): Promise<void> =>
        axiosClient.delete(`/v1/reception/boarding/care-logs/${careLogId}`),

    uploadCareLogMedia: (careLogId: string, image: File): Promise<void> => {
        const formData = new FormData();
        formData.append("file", image);
        return axiosClient.post(`/v1/reception/boarding/care-logs/${careLogId}/media`, formData, {
            headers: { "Content-Type": "multipart/form-data" },
        });
    },
};

export const roomAdminApi = {
    getRoomTypes: (): Promise<RoomTypeResponse[]> => axiosClient.get("/v1/room-types"),

    createRoomType: (data: RoomTypeRequest): Promise<RoomTypeResponse> =>
        axiosClient.post("/v1/room-types", data),

    updateRoomType: (id: string, data: RoomTypeRequest): Promise<RoomTypeResponse> =>
        axiosClient.put(`/v1/room-types/${id}`, data),

    deleteRoomType: (id: string): Promise<void> => axiosClient.delete(`/v1/room-types/${id}`),

    getRooms: async (): Promise<PageResponse<RoomResponse>> => {
        const response = await axiosClient.get<any, PageEnvelope<RoomResponse>>("/v1/rooms");
        return unwrapPage(response);
    },

    createRoom: (data: RoomRequest): Promise<RoomResponse> =>
        axiosClient.post("/v1/rooms", data),

    updateRoom: (id: string, data: RoomRequest): Promise<RoomResponse> =>
        axiosClient.put(`/v1/rooms/${id}`, data),

    updateRoomStatus: (id: string, statusCode: RoomStatus): Promise<RoomResponse> =>
        axiosClient.patch(`/v1/rooms/${id}/status`, { statusCode }),
};
