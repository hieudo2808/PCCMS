import axios, { type AxiosError, type InternalAxiosRequestConfig, type AxiosResponse } from "axios";
import toast from "react-hot-toast";
import { router } from "~/router";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";

const axiosClient = axios.create({
    baseURL: API_BASE_URL,
    withCredentials: true,
    headers: {
        "Content-Type": "application/json",
    },
});

const tokenRefreshClient = axios.create({
    baseURL: API_BASE_URL,
    withCredentials: true,
    headers: {
        "Content-Type": "application/json",
    },
});

let isRefreshing = false;
let failedQueue: { resolve: (token: string) => void; reject: (error: any) => void }[] = [];

const processQueue = (error: any, token: string | null = null) => {
    failedQueue.forEach((prom) => {
        if (error) {
            prom.reject(error);
        } else if (token) {
            prom.resolve(token);
        }
    });

    failedQueue = [];
};

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
        const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean };

        if (error.response?.status === 401 && originalRequest && !originalRequest._retry) {
            const url = originalRequest.url || "";
            const isAuthEndpoint = url.includes("/auth/login") || url.includes("/auth/register") || url.includes("/auth/refresh");
            
            if (isAuthEndpoint) {
                return Promise.reject(error);
            }

            if (isRefreshing) {
                return new Promise(function (resolve, reject) {
                    failedQueue.push({ resolve, reject });
                })
                    .then((token) => {
                        originalRequest.headers.set("Authorization", `Bearer ${token}`);
                        return axiosClient(originalRequest);
                    })
                    .catch((err) => {
                        return Promise.reject(err);
                    });
            }

            originalRequest._retry = true;
            isRefreshing = true;

            try {
                const response = await tokenRefreshClient.post("/v1/auth/refresh");
                const newToken = response.data?.data?.token || response.data?.token;

                if (newToken) {
                    localStorage.setItem("token", newToken);
                    axiosClient.defaults.headers.common["Authorization"] = `Bearer ${newToken}`;
                    originalRequest.headers.set("Authorization", `Bearer ${newToken}`);
                    processQueue(null, newToken);
                    return axiosClient(originalRequest);
                } else {
                    throw new Error("No token returned from refresh endpoint");
                }
            } catch (refreshError) {
                processQueue(refreshError, null);
                localStorage.removeItem("token");
                localStorage.removeItem("user");
                if (typeof window !== "undefined" && window.location.pathname !== "/login") {
                    router.navigate("/login");
                }
                return Promise.reject(refreshError);
            } finally {
                isRefreshing = false;
            }
        } else if (error.response?.status === 403) {
            toast.error("Không có quyền truy cập");
        }

        return Promise.reject(error);
    }
);

export default axiosClient;
