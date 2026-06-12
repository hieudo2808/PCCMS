import api, { getApiData, getPageContent } from "~/api/api";

export interface MedicineSuggestion {
    id: string;
    medicineCode: string;
    name: string;
    categoryId?: string;
    categoryName?: string;
    unit: string;
    defaultInstruction?: string;
    currentStock: number;
    unitPriceVnd?: number;
    isActive?: boolean;
}

export const medicineApi = {
    suggestMedicines: async (keyword: string): Promise<MedicineSuggestion[]> => {
        const response = await api.get("/v1/medicines/suggestions", {
            params: { keyword: keyword || undefined, activeOnly: true, page: 0, size: 10 },
        });
        return getPageContent<MedicineSuggestion>(getApiData<unknown>(response));
    },
};
