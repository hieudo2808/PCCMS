import { describe, it, expect, vi, beforeEach } from 'vitest';
import { medicineApi } from '~/features/admin/api/medicineApi';
import axiosClient from '~/shared/api/axiosClient';
import type { MedicineResponse, CreateMedicineRequest, UpdateMedicineRequest, AddStockRequest } from '~/types/medicine';
import type { PageResponse } from '~/types/api';

vi.mock('~/shared/api/axiosClient', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    patch: vi.fn(),
    delete: vi.fn(),
  },
}));

describe('medicineApi', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  const mockMedicine: MedicineResponse = {
    id: 'med-123',
    medicineCode: 'MED-001',
    name: 'Amoxicillin',
    categoryId: 'cat-123',
    categoryName: 'Kháng sinh',
    unit: 'Hộp',
    defaultInstruction: 'Ngày 2 lần',
    currentStock: 100,
    unitPriceVnd: 50000,
    isActive: true,
  };

  const mockPageResponse: PageResponse<MedicineResponse> = {
    content: [mockMedicine],
    pageNumber: 1,
    pageSize: 10,
    totalElements: 1,
    totalPages: 1,
    isLast: true,
  };

  it('getMedicines should fetch list of medicines', async () => {
    vi.mocked(axiosClient.get).mockResolvedValue(mockPageResponse);
    const result = await medicineApi.getMedicines(1, 10);
    expect(axiosClient.get).toHaveBeenCalledWith('/v1/medicines', { params: { page: 0, size: 10 } });
    expect(result).toEqual(mockPageResponse);
  });

  it('getMedicineById should fetch medicine details', async () => {
    vi.mocked(axiosClient.get).mockResolvedValue(mockMedicine);
    const result = await medicineApi.getMedicineById('med-123');
    expect(axiosClient.get).toHaveBeenCalledWith('/v1/medicines/med-123');
    expect(result).toEqual(mockMedicine);
  });

  it('createMedicine should post new medicine', async () => {
    const request: CreateMedicineRequest = {
      medicineCode: 'MED-001',
      name: 'Amoxicillin',
      categoryId: 'cat-123',
      unit: 'Hộp',
      currentStock: 100,
      unitPriceVnd: 50000,
    };
    vi.mocked(axiosClient.post).mockResolvedValue(mockMedicine);
    const result = await medicineApi.createMedicine(request);
    expect(axiosClient.post).toHaveBeenCalledWith('/v1/medicines', request);
    expect(result).toEqual(mockMedicine);
  });

  it('updateMedicine should put updated medicine', async () => {
    const request: UpdateMedicineRequest = {
      medicineCode: 'MED-001',
      name: 'Amox 500mg',
      categoryId: 'cat-123',
      unit: 'Hộp',
      currentStock: 100,
      unitPriceVnd: 50000,
    };
    vi.mocked(axiosClient.put).mockResolvedValue({ ...mockMedicine, name: 'Amox 500mg' });
    const result = await medicineApi.updateMedicine('med-123', request);
    expect(axiosClient.put).toHaveBeenCalledWith('/v1/medicines/med-123', request);
    expect(result.name).toEqual('Amox 500mg');
  });

  it('addStock should patch medicine stock', async () => {
    const request: AddStockRequest = { quantityToAdd: 50 };
    vi.mocked(axiosClient.patch).mockResolvedValue({ ...mockMedicine, currentStock: 150 });
    const result = await medicineApi.addStock('med-123', request);
    expect(axiosClient.patch).toHaveBeenCalledWith('/v1/medicines/med-123/stock', request);
    expect(result.currentStock).toEqual(150);
  });

  it('deleteMedicine should delete medicine', async () => {
    vi.mocked(axiosClient.delete).mockResolvedValue(undefined);
    await medicineApi.deleteMedicine('med-123');
    expect(axiosClient.delete).toHaveBeenCalledWith('/v1/medicines/med-123');
  });
});
