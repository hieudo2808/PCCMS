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
    axiosClient.get("/api/v1/boarding/availability", { params: { startAt, endAt } }),

  createBooking: (data: CreateBoardingBookingRequest): Promise<BoardingBookingResponse> =>
    axiosClient.post("/api/v1/boarding/bookings", data),

  getMyBookings: async (): Promise<PageResponse<BoardingBookingResponse>> => {
    const response = await axiosClient.get<any, PageEnvelope<BoardingBookingResponse>>("/api/v1/boarding/bookings/my");
    return unwrapPage(response);
  },

  getBookings: async (statusCode?: BoardingStatus): Promise<PageResponse<BoardingBookingResponse>> => {
    const response = await axiosClient.get<any, PageEnvelope<BoardingBookingResponse>>("/api/v1/boarding/bookings", {
      params: statusCode ? { statusCode } : undefined,
    });
    return unwrapPage(response);
  },

  confirmBooking: (bookingId: string, roomId: string): Promise<BoardingBookingResponse> =>
    axiosClient.post(`/api/v1/boarding/bookings/${bookingId}/confirmations`, { roomId }),

  checkIn: (bookingId: string): Promise<BoardingBookingResponse> =>
    axiosClient.post(`/api/v1/boarding/bookings/${bookingId}/check-ins`),

  startStay: (bookingId: string): Promise<BoardingBookingResponse> =>
    axiosClient.post(`/api/v1/boarding/bookings/${bookingId}/stay-starts`),

  checkOut: (bookingId: string): Promise<BoardingBookingResponse> =>
    axiosClient.post(`/api/v1/boarding/bookings/${bookingId}/check-outs`),

  cancelBooking: (bookingId: string, reason: string): Promise<BoardingBookingResponse> =>
    axiosClient.post(`/api/v1/boarding/bookings/${bookingId}/cancellations`, { reason }),

  createCareLog: (payload: CareLogFormPayload): Promise<CareLogResponse> => {
    const formData = new FormData();
    formData.append("logDate", payload.logDate);
    formData.append("periodCode", payload.periodCode);
    formData.append("feedingStatus", payload.feedingStatus);
    formData.append("hygieneStatus", payload.hygieneStatus);
    if (payload.healthNote) formData.append("healthNote", payload.healthNote);
    if (payload.staffNote) formData.append("staffNote", payload.staffNote);
    if (payload.caption) formData.append("caption", payload.caption);
    payload.images?.forEach((image) => formData.append("images", image));
    return axiosClient.post(`/api/v1/boarding/sessions/${payload.sessionId}/care-logs`, formData, {
      headers: { "Content-Type": "multipart/form-data" },
    });
  },

  getCareLogs: (bookingId: string): Promise<CareLogResponse[]> =>
    axiosClient.get(`/api/v1/boarding/bookings/${bookingId}/care-logs`),
};

export const roomAdminApi = {
  getRoomTypes: (): Promise<RoomTypeResponse[]> => axiosClient.get("/api/v1/room-types"),

  createRoomType: (data: RoomTypeRequest): Promise<RoomTypeResponse> => axiosClient.post("/api/v1/room-types", data),

  updateRoomType: (id: string, data: RoomTypeRequest): Promise<RoomTypeResponse> =>
    axiosClient.put(`/api/v1/room-types/${id}`, data),

  deleteRoomType: (id: string): Promise<void> => axiosClient.delete(`/api/v1/room-types/${id}`),

  getRooms: async (): Promise<PageResponse<RoomResponse>> => {
    const response = await axiosClient.get<any, PageEnvelope<RoomResponse>>("/api/v1/rooms");
    return unwrapPage(response);
  },

  createRoom: (data: RoomRequest): Promise<RoomResponse> => axiosClient.post("/api/v1/rooms", data),

  updateRoom: (id: string, data: RoomRequest): Promise<RoomResponse> => axiosClient.put(`/api/v1/rooms/${id}`, data),

  updateRoomStatus: (id: string, statusCode: RoomStatus): Promise<RoomResponse> =>
    axiosClient.patch(`/api/v1/rooms/${id}/status`, { statusCode }),
};
