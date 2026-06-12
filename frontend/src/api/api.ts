import { type AxiosResponse } from "axios";
import axiosClient from "../shared/api/axiosClient";

export function getApiData<T>(response: AxiosResponse): T {
    // axiosClient already unwraps response.data.data
    // but just in case we handle both wrapped and unwrapped
    return response.data?.data ?? response.data?.result ?? response.data ?? response;
}

export function getPageContent<T>(payload: unknown): T[] {
    if (payload && typeof payload === "object" && "data" in payload) {
        const pageData = (payload as { data?: { content?: T[] } }).data;
        if (Array.isArray(pageData?.content)) {
            return pageData.content;
        }
    }
    if (payload && typeof payload === "object" && "content" in payload) {
        const content = (payload as { content?: T[] }).content;
        if (Array.isArray(content)) {
            return content;
        }
    }
    return Array.isArray(payload) ? (payload as T[]) : [];
}

const api = axiosClient;
export default api;
