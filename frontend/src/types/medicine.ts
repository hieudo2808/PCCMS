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
  medicineCode: string;
  name: string;
  categoryId: string;
  unit: string;
  defaultInstruction?: string;
  currentStock: number;
  unitPriceVnd: number;
}

export interface UpdateMedicineRequest {
  medicineCode: string;
  name: string;
  categoryId: string;
  unit: string;
  defaultInstruction?: string;
  currentStock: number;
  unitPriceVnd: number;
}

export interface AddStockRequest {
  quantityToAdd: number;
}
