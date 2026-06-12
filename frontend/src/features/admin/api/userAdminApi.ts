import axiosClient from "~/shared/api/axiosClient";
import type { PageResponse } from "~/types/api";
import type { CreateUserRequest, UpdateUserRequest, UserResponse } from "~/types/user";

interface GetUsersParams {
    page?: number;
    limit?: number;
    role?: string;
    search?: string;
}

export const userAdminApi = {
    getUsers: async (params?: GetUsersParams): Promise<PageResponse<UserResponse>> => {
        return axiosClient.get<any, PageResponse<UserResponse>>("/v1/admin/accounts", {
            params: {
                keyword: params?.search,
                role: params?.role,
                page: params?.page != null ? Math.max(0, params.page - 1) : undefined,
                size: params?.limit,
            },
        });
    },

    createUser: async (data: CreateUserRequest): Promise<UserResponse> => {
        const response = await axiosClient.post<UserResponse>("/v1/admin/accounts", data);
        return response as unknown as UserResponse;
    },

    updateUser: async (id: string, data: UpdateUserRequest): Promise<UserResponse> => {
        const response = await axiosClient.put(`/v1/admin/accounts/${id}`, data);
        return response as unknown as UserResponse;
    },

    lockUser: async (id: string): Promise<void> => {
        await axiosClient.patch(`/v1/admin/accounts/${id}/status`, { statusCode: "LOCKED" });
    },

    disableUser: async (id: string): Promise<void> => {
        await axiosClient.patch(`/v1/admin/accounts/${id}/status`, { statusCode: "DISABLED" });
    },
};
