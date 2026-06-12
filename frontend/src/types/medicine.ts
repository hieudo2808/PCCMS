export interface MedicineResponse {
    id: string;
    medicineCode: string;
    name: string;
    categoryId: string;
    categoryName: string;
    unit: string;
    defaultInstruction: string;
    currentStock: number;
    unitPriceVnd: number;
    isActive: boolean;
}

export interface CreateMedicineRequest {
    name: string;
    categoryId: string;
    unit: string;
    defaultInstruction?: string;
    initialStock: number;
    unitPriceVnd: number;
}

export interface UpdateMedicineRequest {
    name?: string;
    categoryId?: string;
    unit?: string;
    defaultInstruction?: string;
    unitPriceVnd?: number;
}

export interface AddStockRequest {
    quantity: number;
    note?: string;
}
