import api, { getApiData } from "~/api/api";
import type { PageResponse } from "~/types/api";
import { normalizePage } from "~/shared/api/pageUtils";

export interface AdminShiftChangeRequestResponse {
    id: string;
    scheduleId: string;
    requestedBy: string;
    targetStaff?: string;
    workDate: string;
    shiftName: string;
    reason: string;
    statusCode: "PENDING" | "ACCEPTED" | "REJECTED" | "CANCELLED";
    resolvedBy?: string;
    resolvedAt?: string;
    createdAt: string;
}

export interface AdminShiftChangeRequestSearchParams {
    statusCode?: string;
    page?: number;
    size?: number;
}

export const getAdminShiftChangeRequests = async (params: AdminShiftChangeRequestSearchParams = {}): Promise<PageResponse<AdminShiftChangeRequestResponse>> => {
    const response = await api.get("/v1/admin/shift-change-requests", {
        params: {
            statusCode: params.statusCode || undefined,
            page: params.page ?? 0,
            size: params.size ?? 20,
        },
    });

    return normalizePage<AdminShiftChangeRequestResponse>(response);
};

export const respondToAdminShiftChangeRequest = async (requestId: string, action: "ACCEPTED" | "REJECTED") => {
    const response = await api.patch(`/v1/admin/shift-change-requests/${requestId}/status`, { statusCode: action });
    return getApiData<AdminShiftChangeRequestResponse>(response);
};
