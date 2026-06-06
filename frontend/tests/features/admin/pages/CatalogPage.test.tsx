import { render, screen, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { CatalogPage } from '~/features/admin/pages/CatalogPage';
import { medicineApi } from '~/features/admin/api/medicineApi';
import { catalogApi } from '~/features/admin/api/catalogApi';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import userEvent from '@testing-library/user-event';
import type { PageResponse } from '~/types/api';
import type { MedicineResponse } from '~/types/medicine';

vi.mock('~/features/admin/api/medicineApi', () => ({
  medicineApi: {
    getMedicines: vi.fn(),
    createMedicine: vi.fn(),
    updateMedicine: vi.fn(),
    deleteMedicine: vi.fn(),
    addStock: vi.fn(),
  },
}));

vi.mock('~/features/admin/api/catalogApi', () => ({
  catalogApi: {
    listServices: vi.fn(),
    createService: vi.fn(),
    updateService: vi.fn(),
    deleteService: vi.fn(),
    listMedicineCategories: vi.fn(),
    createMedicineCategory: vi.fn(),
    updateMedicineCategory: vi.fn(),
    deleteMedicineCategory: vi.fn(),
  },
  medicineCategoryApi: {
    listCategories: vi.fn(),
  },
}));

vi.mock('react-hot-toast', () => ({
  toast: {
    success: vi.fn(),
    error: vi.fn(),
  },
}));

const mockMedicines: MedicineResponse[] = [
  {
    id: 'med-1',
    medicineCode: 'MED-001',
    name: 'Amoxicillin',
    categoryId: 'cat-1',
    categoryName: 'Kháng sinh',
    unit: 'Hộp',
    defaultInstruction: 'Ngày 2 lần',
    currentStock: 124,
    unitPriceVnd: 100000,
    isActive: true,
  },
];

const mockPageResponse: PageResponse<MedicineResponse> = {
  content: mockMedicines,
  pageNumber: 1,
  pageSize: 10,
  totalElements: 1,
  totalPages: 1,
  isLast: true,
};

const renderWithProviders = (ui: React.ReactElement) => {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
      },
    },
  });

  return render(
    <QueryClientProvider client={queryClient}>
      {ui}
    </QueryClientProvider>
  );
};

describe('CatalogPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(catalogApi.listMedicineCategories).mockResolvedValue([
      { id: 'cat-1', name: 'Kháng sinh', isActive: true },
    ]);
    vi.mocked(catalogApi.listServices).mockResolvedValue({
      content: [],
      pageNumber: 1,
      pageSize: 10,
      totalElements: 0,
      totalPages: 0,
      isLast: true,
    });
  });

  it('renders loading state initially', () => {
    vi.mocked(medicineApi.getMedicines).mockReturnValue(new Promise(() => {}));
    const { container } = renderWithProviders(<CatalogPage />);
    expect(container.querySelector('.animate-spin')).toBeInTheDocument();
  });

  it('displays list of medicines', async () => {
    vi.mocked(medicineApi.getMedicines).mockResolvedValue(mockPageResponse);
    renderWithProviders(<CatalogPage />);

    await waitFor(() => {
      expect(screen.getByText('Amoxicillin')).toBeInTheDocument();
    });
    expect(screen.getByText('124')).toBeInTheDocument();
  });

  it('opens create modal when clicking add button', async () => {
    vi.mocked(medicineApi.getMedicines).mockResolvedValue(mockPageResponse);
    renderWithProviders(<CatalogPage />);

    await waitFor(() => {
      expect(screen.getByText('Amoxicillin')).toBeInTheDocument();
    });

    const addBtn = screen.getByRole('button', { name: /Thêm thuốc mới/i });
    await userEvent.click(addBtn);

    expect(screen.getByRole('dialog')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: /Thêm thuốc mới/i })).toBeInTheDocument();
  });

  it('submits create medicine form', async () => {
    vi.mocked(medicineApi.getMedicines).mockResolvedValue(mockPageResponse);
    vi.mocked(medicineApi.createMedicine).mockResolvedValue(mockMedicines[0]);
    renderWithProviders(<CatalogPage />);

    await waitFor(() => {
      expect(screen.getByText('Amoxicillin')).toBeInTheDocument();
    });

    const addBtn = screen.getByRole('button', { name: /Thêm thuốc mới/i });
    await userEvent.click(addBtn);

    await userEvent.type(screen.getByLabelText(/Mã thuốc/i), 'MED-002');
    await userEvent.type(screen.getByLabelText(/Tên thuốc/i), 'Vitamin C');
    await userEvent.selectOptions(screen.getByLabelText(/Nhóm thuốc/i), 'cat-1');
    await userEvent.type(screen.getByLabelText(/Đơn vị/i), 'Vỉ');
    await userEvent.type(screen.getByLabelText(/Giá/i), '50000');
    await userEvent.type(screen.getByLabelText(/Tồn kho/i), '100');
    await userEvent.type(screen.getByLabelText(/Hướng dẫn dùng mặc định/i), 'Ngày 1 lần');

    const submitBtn = screen.getByRole('button', { name: /Lưu/i });
    await userEvent.click(submitBtn);

    await waitFor(() => {
      expect(medicineApi.createMedicine).toHaveBeenCalled();
      expect(medicineApi.createMedicine.mock.calls[0][0]).toEqual({
        medicineCode: 'MED-002',
        name: 'Vitamin C',
        categoryId: 'cat-1',
        unit: 'Vỉ',
        unitPriceVnd: 50000,
        currentStock: 100,
        defaultInstruction: 'Ngày 1 lần',
      });
    });
  });
});
