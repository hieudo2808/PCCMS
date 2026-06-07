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
    email: string;
    password?: string;
    fullName?: string;
}
