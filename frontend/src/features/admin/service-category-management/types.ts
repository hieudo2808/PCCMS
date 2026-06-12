export type ServiceStatus = "active" | "inactive";

export type ServiceType = "Khám bệnh" | "Làm đẹp" | "Lưu trú" | "Khác";

export interface Service {
    id: string;
    code: string;
    name: string;
    type: ServiceType;
    price: number;
    durationMinutes: number;
    description: string;
    status: ServiceStatus;
    isReferenced?: boolean;
}

export interface ServiceSearchParams {
    keyword: string;
    type: ServiceType | "";
    status: ServiceStatus | "";
}

export interface ServiceFormValues {
    code: string;
    name: string;
    type: ServiceType | "";
    price: string;
    durationMinutes: string;
    description: string;
    status: ServiceStatus | "";
}
