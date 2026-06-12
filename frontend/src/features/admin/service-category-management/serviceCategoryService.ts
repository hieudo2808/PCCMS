import api, { getApiData, getPageContent } from "~/api/api";
import type { Service, ServiceFormValues, ServiceSearchParams, ServiceStatus, ServiceType } from "./types";

type BackendServiceCategory = "MEDICAL" | "GROOMING" | "BOARDING" | "OTHER";

interface BackendService {
    id: string;
    serviceCode: string;
    name: string;
    categoryCode: BackendServiceCategory;
    description?: string;
    basePriceVnd: number;
    durationMinutes?: number;
    isActive?: boolean;
}

const typeToBackend: Record<ServiceType, BackendServiceCategory> = {
    "Khám bệnh": "MEDICAL",
    "Làm đẹp": "GROOMING",
    "Lưu trú": "BOARDING",
    Khác: "OTHER",
};

const typeFromBackend = (category: BackendServiceCategory): ServiceType => {
    switch (category) {
        case "GROOMING":
            return "Làm đẹp";
        case "BOARDING":
            return "Lưu trú";
        case "OTHER":
            return "Khác";
        default:
            return "Khám bệnh";
    }
};

function toService(item: BackendService): Service {
    return {
        id: item.id,
        code: item.serviceCode,
        name: item.name,
        type: typeFromBackend(item.categoryCode),
        price: item.basePriceVnd,
        durationMinutes: item.durationMinutes ?? 0,
        description: item.description ?? "",
        status: item.isActive === false ? "inactive" : "active",
    };
}

function toPayload(payload: ServiceFormValues) {
    return {
        serviceCode: payload.code.trim(),
        name: payload.name.trim(),
        categoryCode: payload.type ? typeToBackend[payload.type] : "OTHER",
        description: payload.description.trim(),
        basePriceVnd: Number(payload.price),
        durationMinutes: payload.durationMinutes.trim() ? Number(payload.durationMinutes) : null,
        isActive: (payload.status as ServiceStatus | "") !== "inactive",
        effectiveFrom: null,
        effectiveTo: null,
    };
}

export const getServices = async () => {
    const response = await api.get("/v1/catalog/services", { params: { isActive: true, page: 0, size: 50 } });
    return getPageContent<BackendService>(getApiData<unknown>(response)).map(toService);
};

export const searchServices = async (params: ServiceSearchParams) => {
    try {
        const response = await api.get("/v1/catalog/services", {
            params: {
                keyword: params.keyword || undefined,
                categoryCode: params.type ? typeToBackend[params.type] : undefined,
                isActive: params.status ? params.status === "active" : true,
                page: 0,
                size: 50,
            },
        });
        return getPageContent<BackendService>(getApiData<unknown>(response)).map(toService);
    } catch {
        throw new Error("Không thể tải danh sách dịch vụ. Vui lòng thử lại.");
    }
};

export const createService = async (payload: ServiceFormValues) => {
    const response = await api.post("/v1/catalog/services", toPayload(payload));
    return toService(getApiData<BackendService>(response));
};

export const updateService = async (id: string, payload: ServiceFormValues) => {
    const response = await api.put(`/v1/catalog/services/${id}`, toPayload(payload));
    return toService(getApiData<BackendService>(response));
};

export const deleteService = async (id: string) => {
    await api.delete(`/v1/catalog/services/${id}`);
};
