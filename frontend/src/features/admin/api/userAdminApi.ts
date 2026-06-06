import axiosClient from '~/shared/api/axiosClient';
import type { PageResponse } from '~/types/api';
import type { CreateUserRequest, UpdateUserRequest, UserResponse } from '~/types/user';

interface GetUsersParams {
  page?: number;
  limit?: number;
  role?: string;
  search?: string;
}

export const userAdminApi = {
  getUsers: async (params?: GetUsersParams): Promise<PageResponse<UserResponse>> => {
    const users = await axiosClient.get<UserResponse[]>('/users', { params });
    const content = users as unknown as UserResponse[];
    return {
      content,
      pageNumber: params?.page ?? 1,
      pageSize: params?.limit ?? content.length,
      totalElements: content.length,
      totalPages: 1,
      isLast: true,
    };
  },

  createUser: async (data: CreateUserRequest): Promise<UserResponse> => {
    const response = await axiosClient.post<UserResponse>('/users', data);
    return response as unknown as UserResponse;
  },

  updateUser: async (id: string, data: UpdateUserRequest): Promise<UserResponse> => {
    const response = await axiosClient.put<UserResponse>(`/users/${id}`, data);
    return response as unknown as UserResponse;
  },

  lockUser: async (id: string): Promise<void> => {
    await axiosClient.patch(`/users/${id}/lock`);
  },

  disableUser: async (id: string): Promise<void> => {
    await axiosClient.patch(`/users/${id}/disable`);
  },
};
