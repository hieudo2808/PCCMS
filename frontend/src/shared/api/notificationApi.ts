import axiosClient from "~/shared/api/axiosClient";

export interface NotificationResponse {
    id: string;
    recipientId: string;
    sourceType: string;
    sourceId: string;
    notificationType: string;
    title: string;
    body: string;
    statusCode: string;
    readAt: string | null;
    createdAt: string;
    updatedAt: string;
}

export interface PageResponse<T> {
    content: T[];
    totalElements: number;
    totalPages: number;
    size: number;
    number: number;
}

export const notificationApi = {
    listMyNotifications: async (params?: {
        page?: number;
        size?: number;
    }): Promise<PageResponse<NotificationResponse>> => {
        const response = await axiosClient.get<unknown, PageResponse<NotificationResponse>>("/v1/notifications/my", {
            params: {
                page: params?.page ?? 0,
                size: params?.size ?? 10,
            },
        });
        return response;
    },
};
