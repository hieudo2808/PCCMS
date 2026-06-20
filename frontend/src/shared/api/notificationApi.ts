import axiosClient from "~/shared/api/axiosClient";

export type NotificationStatus = "UNREAD" | "READ" | "ARCHIVED";

export interface NotificationResponse {
    id: string;
    recipientUserId: string;
    sourceType: string | null;
    sourceId: string | null;
    notificationType: string;
    title: string;
    body: string;
    statusCode: NotificationStatus;
    readAt: string | null;
    createdAt: string;
    updatedAt: string;
}

export interface PageResponse<T> {
    content: T[];
    pageNumber: number;
    pageSize: number;
    totalElements: number;
    totalPages: number;
    isFirst: boolean;
    isLast: boolean;
}

type PageEnvelope<T> = PageResponse<T> | { success: boolean; data: PageResponse<T> };

function unwrapPage<T>(value: PageEnvelope<T>): PageResponse<T> {
    if ("data" in value && value.data && "content" in value.data) {
        return value.data as PageResponse<T>;
    }
    return value as PageResponse<T>;
}

export interface NotificationListParams {
    page?: number;
    size?: number;
    status?: NotificationStatus;
}

export const notificationKeys = {
    all: ["notifications"] as const,
    list: (params: NotificationListParams) => [...notificationKeys.all, "list", params] as const,
    unreadCount: () => [...notificationKeys.all, "unread-count"] as const,
};

export const notificationApi = {
    listMyNotifications: async (params: NotificationListParams = {}) => {
        const response = await axiosClient.get<unknown, PageEnvelope<NotificationResponse>>("/v1/notifications/my", {
            params: { page: params.page ?? 0, size: params.size ?? 20, status: params.status },
        });
        return unwrapPage(response);
    },
    getUnreadCount: () =>
        axiosClient.get<unknown, { unreadCount: number }>("/v1/notifications/unread-count"),
    markRead: (notificationId: string) =>
        axiosClient.patch<unknown, NotificationResponse>(`/v1/notifications/${notificationId}/read`),
    archive: (notificationId: string) =>
        axiosClient.patch<unknown, NotificationResponse>(`/v1/notifications/${notificationId}/archive`),
    markAllRead: () =>
        axiosClient.patch<unknown, { updatedCount: number }>("/v1/notifications/read-all"),
};
