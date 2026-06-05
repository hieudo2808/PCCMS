import axios, { type AxiosError, type InternalAxiosRequestConfig, type AxiosResponse } from 'axios';
import toast from 'react-hot-toast';
import { notifyUnauthorized, recordAuthFailure } from '~/shared/auth/authSession';
import { getAccessToken, hasAccessToken } from '~/shared/auth/tokenStorage';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

const axiosClient = axios.create({
  baseURL: API_BASE_URL,
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json',
  },
});

type RetryConfig = InternalAxiosRequestConfig & { _retry?: boolean };

let refreshPromise: Promise<string | null> | null = null;

async function refreshAccessToken(): Promise<string | null> {
  if (!refreshPromise) {
    refreshPromise = axios
      .post(
        `${API_BASE_URL}/auth/refresh`,
        {},
        { withCredentials: true, headers: { 'Content-Type': 'application/json' } }
      )
      .then((res) => {
        const body = res.data as { data?: { token?: string; user?: unknown } };
        const token = body?.data?.token;
        if (token) {
          localStorage.setItem('token', token);
          if (body.data?.user) {
            localStorage.setItem('user', JSON.stringify(body.data.user));
          }
          return token;
        }
        return null;
      })
      .catch(() => null)
      .finally(() => {
        refreshPromise = null;
      });
  }
  return refreshPromise;
}

axiosClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = getAccessToken();
    if (token) {
      config.headers.set('Authorization', `Bearer ${token}`);
    }
    return config;
  },
  (error: AxiosError) => Promise.reject(error)
);

axiosClient.interceptors.response.use(
  (response: AxiosResponse) => {
    if (response.data && 'data' in response.data && 'success' in response.data) {
      return response.data.data;
    }
    return response.data;
  },
  async (error: AxiosError) => {
    const status = error.response?.status;
    const config = error.config as RetryConfig | undefined;

    if (status === 401 && config && !config._retry) {
      const hadToken = hasAccessToken();

      if (hadToken) {
        config._retry = true;
        const newToken = await refreshAccessToken();
        if (newToken) {
          config.headers.set('Authorization', `Bearer ${newToken}`);
          return axiosClient(config);
        }
      }

      recordAuthFailure({
        source: 'api-401',
        url: config.url,
        method: config.method?.toUpperCase(),
        status: 401,
        responseBody: error.response?.data,
        message: hadToken
          ? 'API 401 sau khi gửi token — cần đăng nhập lại (restart backend nếu vừa sửa JWT)'
          : 'API 401 — không có token (đăng nhập trước khi vào trang)',
      });

      if (hadToken) {
        notifyUnauthorized();
        toast.error('Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.');
      }
    } else if (status === 403) {
      recordAuthFailure({
        source: 'api-401',
        url: config?.url,
        method: config?.method?.toUpperCase(),
        status: 403,
        responseBody: error.response?.data,
        message: 'API trả 403 — không đủ quyền',
      });
      toast.error('Không có quyền truy cập');
    }

    return Promise.reject(error);
  }
);

export default axiosClient;
