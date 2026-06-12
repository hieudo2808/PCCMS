import axiosClient from "~/shared/api/axiosClient";
import type { PageResponse } from "~/types/api";
import type {
    CreateGroomingBookingRequest,
    GroomingServiceRequest,
    GroomingServiceResponse,
    GroomingStationRequest,
    GroomingStationResponse,
    GroomingStatus,
    GroomingTicketResponse,
} from "~/types/grooming";

type PageEnvelope<T> = PageResponse<T> | { success: boolean; data: PageResponse<T> };

function unwrapPage<T>(value: PageEnvelope<T>): PageResponse<T> {
    if ("data" in value && value.data && "content" in value.data) {
        return value.data;
    }
    return value as PageResponse<T>;
}

export const groomingApi = {
    getServices: (): Promise<GroomingServiceResponse[]> =>
        axiosClient.get("/v1/grooming/services"),

    getStations: (): Promise<GroomingStationResponse[]> =>
        axiosClient.get("/v1/grooming/stations"),

    createBooking: (data: CreateGroomingBookingRequest): Promise<GroomingTicketResponse> =>
        axiosClient.post("/v1/grooming/tickets", data),

    getMyTickets: async (): Promise<PageResponse<GroomingTicketResponse>> => {
        const response = await axiosClient.get<any, PageEnvelope<GroomingTicketResponse>>(
            "/v1/grooming/tickets/my"
        );
        return unwrapPage(response);
    },

    getTickets: async (
        params?: { statusCode?: GroomingStatus; page?: number; size?: number }
    ): Promise<PageResponse<GroomingTicketResponse>> => {
        const response = await axiosClient.get<any, PageEnvelope<GroomingTicketResponse>>(
            "/v1/grooming/tickets",
            {
                params,
            }
        );
        return unwrapPage(response);
    },

    confirmTicket: (ticketId: string, stationId: string): Promise<GroomingTicketResponse> =>
        axiosClient.post(`/v1/grooming/tickets/${ticketId}/confirmations`, { stationId }),

    startTicket: (ticketId: string): Promise<GroomingTicketResponse> =>
        axiosClient.post(`/v1/grooming/tickets/${ticketId}/starts`),

    completeTicket: (ticketId: string, internalNote?: string): Promise<GroomingTicketResponse> =>
        axiosClient.post(`/v1/grooming/tickets/${ticketId}/completions`, { internalNote }),

    cancelTicket: (ticketId: string, reason: string): Promise<GroomingTicketResponse> =>
        axiosClient.post(`/v1/grooming/tickets/${ticketId}/cancellations`, { reason }),

    getAdminServices: (): Promise<GroomingServiceResponse[]> =>
        axiosClient.get("/v1/grooming/admin/services"),

    createAdminService: (data: GroomingServiceRequest): Promise<GroomingServiceResponse> =>
        axiosClient.post("/v1/grooming/admin/services", data),

    updateAdminService: (
        id: string,
        data: GroomingServiceRequest
    ): Promise<GroomingServiceResponse> =>
        axiosClient.put(`/v1/grooming/admin/services/${id}`, data),

    deactivateAdminService: (id: string): Promise<void> =>
        axiosClient.delete(`/v1/grooming/admin/services/${id}`),

    getAdminStations: (): Promise<GroomingStationResponse[]> =>
        axiosClient.get("/v1/grooming/admin/stations"),

    createStation: (data: GroomingStationRequest): Promise<GroomingStationResponse> =>
        axiosClient.post("/v1/grooming/admin/stations", data),

    updateStation: (id: string, data: GroomingStationRequest): Promise<GroomingStationResponse> =>
        axiosClient.put(`/v1/grooming/admin/stations/${id}`, data),

    deactivateStation: (id: string): Promise<void> =>
        axiosClient.patch(`/v1/grooming/admin/stations/${id}/deactivation`),
};
