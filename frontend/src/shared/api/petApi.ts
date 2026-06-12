import axiosClient from "~/shared/api/axiosClient";
import type { PetResponse, PetRequest } from "~/types/pet";
import type { PageResponse } from "~/types/api";

type PageEnvelope<T> =
    | PageResponse<T>
    | { success: boolean; data: PageResponse<T> | { content: T[] } };

function unwrapPage<T>(value: PageEnvelope<T>): PageResponse<T> {
    if ("data" in value && value.data) {
        const nested = value.data as any;
        if ("content" in nested) return nested as PageResponse<T>;
        if (nested.data && "content" in nested.data) return nested.data as PageResponse<T>;
    }
    return value as PageResponse<T>;
}

export const petApi = {
    getPets: async (params?: {
        ownerId?: string;
        page?: number;
        size?: number;
        isActive?: boolean;
    }): Promise<PageResponse<PetResponse>> => {
        const response = await axiosClient.get<any, PageEnvelope<PetResponse>>("/v1/pets", {
            params,
        });
        return unwrapPage(response);
    },

    getPetById: (id: string): Promise<PetResponse> => {
        return axiosClient.get(`/v1/pets/${id}`);
    },

    createPet: (data: PetRequest): Promise<PetResponse> => {
        return axiosClient.post("/v1/pets", data);
    },

    updatePet: (id: string, data: PetRequest): Promise<PetResponse> => {
        return axiosClient.put(`/v1/pets/${id}`, data);
    },

    deletePet: (id: string): Promise<void> => {
        return axiosClient.delete(`/v1/pets/${id}`);
    },
};
