import axiosClient from "~/shared/api/axiosClient";
import type { UserResponse, UpdateUserRequest } from "~/types/user";

export interface ChangePasswordPayload {
    currentPassword: string;
    newPassword: string;
}

export interface ContactOtpRequest {
    contact: string;
}

export interface ContactOtpConfirmRequest {
    contact: string;
    otp: string;
}

export const userApi = {
    getProfile: (): Promise<UserResponse> => {
        return axiosClient.get("/v1/users/me");
    },

    updateProfile: (data: UpdateUserRequest): Promise<UserResponse> => {
        return axiosClient.put("/v1/users/me", data);
    },

    changePassword: (data: ChangePasswordPayload): Promise<void> => {
        return axiosClient.put("/v1/users/me/password", data);
    },

    requestEmailChangeOtp: (data: ContactOtpRequest): Promise<void> => {
        return axiosClient.post("/v1/users/me/email-change/otp", data);
    },

    confirmEmailChange: (data: ContactOtpConfirmRequest): Promise<UserResponse> => {
        return axiosClient.post("/v1/users/me/email-change/confirm", data);
    },

    requestPhoneChangeOtp: (data: ContactOtpRequest): Promise<void> => {
        return axiosClient.post("/v1/users/me/phone-change/otp", data);
    },

    confirmPhoneChange: (data: ContactOtpConfirmRequest): Promise<UserResponse> => {
        return axiosClient.post("/v1/users/me/phone-change/confirm", data);
    },
};
