import axiosClient from "../shared/api/axiosClient";

// Re-export axiosClient as default to prevent breaking existing imports
export default axiosClient;

export async function checkMaintenanceStatus(): Promise<boolean> {
    try {
        const response = await axiosClient.get("/maintenance/status");
        return response.data?.maintenance === true;
    } catch {
        return false;
    }
}
