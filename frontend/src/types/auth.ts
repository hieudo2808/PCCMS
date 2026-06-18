import type { UserResponse } from "./user";

export interface AuthResponse {
    token: string;
    refreshToken: string;
    user: UserResponse;
}

export interface LoginRequest {
    email: string;
    password?: string;
}

export interface RegisterRequest {
    fullName: string;
    email: string;
    phone: string;
    password: string;
    confirmPassword?: string;
}
