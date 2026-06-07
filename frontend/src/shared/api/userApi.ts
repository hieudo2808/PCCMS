import axiosClient from "~/shared/api/axiosClient";
import type { UserResponse, UpdateUserRequest } from "~/types/user";

export const userApi = {
    getProfile: (): Promise<UserResponse> => {
        return axiosClient.get("/users/me");
    },

    updateProfile: (data: UpdateUserRequest): Promise<UserResponse> => {
        return axiosClient.put("/users/me", data);
    },

    changePassword: (data: any): Promise<void> => {
        return axiosClient.patch("/users/me/password", data);
    },
};
