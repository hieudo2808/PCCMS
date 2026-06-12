import axiosClient from "../../../shared/api/axiosClient";
import type { LoginRequest, RegisterRequest, AuthResponse } from "../../../types";

export const authApi = {
    login: (data: LoginRequest): Promise<AuthResponse> => {
        return axiosClient.post("/v1/auth/login", data);
    },

    register: (data: RegisterRequest): Promise<AuthResponse> => {
        return axiosClient.post("/v1/auth/register", data);
    },

    refreshToken: (): Promise<AuthResponse> => {
        return axiosClient.post("/v1/auth/refresh");
    },

    requestPasswordResetOtp: (data: { contact: string }): Promise<void> => {
        return axiosClient.post("/v1/auth/password-reset/otp", data);
    },

    confirmPasswordReset: (data: { contact: string; otp: string; newPassword: string }): Promise<void> => {
        return axiosClient.post("/v1/auth/password-reset/confirm", data);
    },
};
