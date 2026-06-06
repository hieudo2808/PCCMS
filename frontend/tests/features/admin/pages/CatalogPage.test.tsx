import { render, screen, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { CatalogPage } from '~/features/admin/pages/CatalogPage';
import { medicineApi } from '~/features/admin/api/medicineApi';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import userEvent from '@testing-library/user-event';
import type { PageResponse } from '~/types/api';
import type { MedicineResponse } from '~/types/medicine';
import { groomingApi } from '~/features/grooming/api/groomingApi';

vi.mock('~/features/admin/api/medicineApi', () => ({
  medicineApi: {
    getMedicines: vi.fn(),
    createMedicine: vi.fn(),
    updateMedicine: vi.fn(),
    deleteMedicine: vi.fn(),
  },
}));

vi.mock('~/features/grooming/api/groomingApi', () => ({
  groomingApi: {
    getAdminServices: vi.fn(),
    getAdminStations: vi.fn(),
    createAdminService: vi.fn(),
    deactivateAdminService: vi.fn(),
    createStation: vi.fn(),
    deactivateStation: vi.fn(),
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
    vi.mocked(groomingApi.getAdminServices).mockResolvedValue([]);
    vi.mocked(groomingApi.getAdminStations).mockResolvedValue([]);
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
    expect(screen.getByText('Thêm Thuốc Mới')).toBeInTheDocument();
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

    // Fill form
    await userEvent.type(screen.getByLabelText(/Tên thuốc/i), 'Vitamin C');
    await userEvent.type(screen.getByLabelText(/Danh mục/i), 'cat-2');
    await userEvent.type(screen.getByLabelText(/Đơn vị/i), 'Vỉ');
    await userEvent.type(screen.getByLabelText(/^Giá \(VND\)$/i), '50000');
    await userEvent.type(screen.getByLabelText(/Tồn kho ban đầu/i), '100');

    const submitBtn = screen.getByRole('button', { name: /^Lưu$/i });
    await userEvent.click(submitBtn);

    await waitFor(() => {
      expect(medicineApi.createMedicine).toHaveBeenCalledWith({
        name: 'Vitamin C',
        categoryId: 'cat-2',
        unit: 'Vỉ',
        unitPriceVnd: 50000,
        initialStock: 100,
        defaultInstruction: '',
      });
    });
  });
});
