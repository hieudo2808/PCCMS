import axios, { type AxiosError, type InternalAxiosRequestConfig, type AxiosResponse } from "axios";
import toast from "react-hot-toast";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";

const axiosClient = axios.create({
    baseURL: API_BASE_URL,
    withCredentials: true,
    headers: {
        "Content-Type": "application/json",
    },
});

axiosClient.interceptors.request.use(
    (config: InternalAxiosRequestConfig) => {
        const token = localStorage.getItem("token");
        if (token) {
            config.headers.set("Authorization", `Bearer ${token}`);
        }
        return config;
    },
    (error: AxiosError) => Promise.reject(error)
);

axiosClient.interceptors.response.use(
    (response: AxiosResponse) => {
        // Unwrap the response data if it matches ApiResponse structure
        if (response.data && "data" in response.data && "success" in response.data) {
            return response.data.data;
        }
        return response.data;
    },
    async (error: AxiosError) => {
        if (error.response?.status === 401) {
            localStorage.removeItem("token");
            // In a real browser environment, redirect to login
            if (typeof window !== "undefined") {
                window.location.href = "/login";
            }
        } else if (error.response?.status === 403) {
            toast.error("Không có quyền truy cập");
        }

        return Promise.reject(error);
    }
);

export default axiosClient;
