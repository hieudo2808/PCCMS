import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { PetProfilesPage } from '~/features/owner/pages/PetProfilesPage';
import { petApi } from '~/shared/api/petApi';
import { petCatalogApi } from '~/shared/api/petCatalogApi';
import { PET_MESSAGES } from '~/features/owner/schema/petSchema';

vi.mock('~/shared/api/petApi');
vi.mock('~/shared/api/petCatalogApi');
vi.mock('react-hot-toast', () => ({
  default: { success: vi.fn(), error: vi.fn() },
}));
vi.mock('~/shared/auth/tokenStorage', () => ({
  hasAccessToken: () => true,
  getAccessToken: () => 'test-token',
}));
vi.mock('~/features/auth/context/AuthContext', () => ({
  useAuth: () => ({
    isAuthenticated: true,
    isInitializing: false,
    user: { id: '1', roleCode: 'OWNER', fullName: 'Test' },
    login: vi.fn(),
    logout: vi.fn(),
  }),
}));

const queryClient = new QueryClient({
  defaultOptions: {
    queries: { retry: false },
  },
});

const renderComponent = () => {
  return render(
    <QueryClientProvider client={queryClient}>
      <PetProfilesPage />
    </QueryClientProvider>
  );
};

const mockPet = {
  id: 'pet-1',
  ownerId: 'owner-1',
  name: 'Milu',
  speciesId: 'species-1',
  speciesName: 'Chó',
  breedId: 'breed-1',
  breedName: 'Poodle',
  sex: 'MALE' as const,
  birthDate: '2023-01-01',
  estimatedAgeMonths: 24,
  weightKg: 5,
  color: 'Brown',
  identificationNote: '',
  specialNote: '',
  allergyNote: '',
  nutritionNote: '',
  isActive: true,
};

const emptyPage = {
  content: [],
  pageNumber: 1,
  pageSize: 20,
  totalElements: 0,
  totalPages: 1,
  isLast: true,
};

describe('PetProfilesPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    queryClient.clear();
    vi.mocked(petCatalogApi.getSpecies).mockResolvedValue([
      { id: 'species-1', name: 'Chó' },
    ]);
    vi.mocked(petCatalogApi.getBreedsBySpecies).mockResolvedValue([
      { id: 'breed-1', speciesId: 'species-1', name: 'Poodle' },
    ]);
    vi.mocked(petApi.getPets).mockResolvedValue(emptyPage);
  });

  it('renders loading state initially', () => {
    vi.mocked(petApi.getPets).mockReturnValue(new Promise(() => {}));
    renderComponent();
    expect(screen.getByText('Hồ sơ thú cưng')).toBeInTheDocument();
  });

  it('renders UC empty state when no pets', async () => {
    renderComponent();

    await waitFor(() => {
      expect(screen.getByText(PET_MESSAGES.emptyList)).toBeInTheDocument();
    });
    expect(screen.getByRole('button', { name: 'Thêm mới ngay' })).toBeInTheDocument();
  });

  it('renders list of pets with avatar area and weight', async () => {
    vi.mocked(petApi.getPets).mockResolvedValueOnce({
      content: [mockPet],
      pageNumber: 1,
      pageSize: 20,
      totalElements: 1,
      totalPages: 1,
      isLast: true,
    });

    renderComponent();

    await waitFor(() => {
      expect(screen.getByText('Milu')).toBeInTheDocument();
    });
    expect(screen.getByText('Poodle')).toBeInTheDocument();
    expect(screen.getByText('5 kg')).toBeInTheDocument();
  });

  it('opens filter menu and filters pets', async () => {
    vi.mocked(petApi.getPets).mockResolvedValue({
      content: [mockPet],
      pageNumber: 1,
      pageSize: 20,
      totalElements: 1,
      totalPages: 1,
      isLast: true,
    });

    renderComponent();

    await waitFor(() => {
      expect(screen.getByText('Milu')).toBeInTheDocument();
    });

    await userEvent.click(screen.getByRole('button', { name: /Bộ lọc/i }));
    await userEvent.click(await screen.findByRole('option', { name: 'Đã ẩn' }));

    await waitFor(() => {
      expect(petApi.getPets).toHaveBeenCalledWith({ isActive: false });
    });
  });

  it('shows UC delete confirmation text', async () => {
    vi.mocked(petApi.getPets).mockResolvedValueOnce({
      content: [mockPet],
      pageNumber: 1,
      pageSize: 20,
      totalElements: 1,
      totalPages: 1,
      isLast: true,
    });
    vi.mocked(petApi.getPetById).mockResolvedValue(mockPet);
    vi.mocked(petApi.deletePet).mockResolvedValueOnce(undefined);

    renderComponent();

    await waitFor(() => {
      expect(screen.getByText('Milu')).toBeInTheDocument();
    });

    await userEvent.click(screen.getByRole('button', { name: 'Xóa hồ sơ' }));

    expect(screen.getByText(PET_MESSAGES.deleteConfirm('Milu'))).toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: 'Xác nhận xóa' }));

    await waitFor(() => {
      expect(petApi.deletePet).toHaveBeenCalledWith('pet-1');
    });
  });

  it('opens detail and edit modals from pet card', async () => {
    vi.mocked(petApi.getPets).mockResolvedValueOnce({
      content: [mockPet],
      pageNumber: 1,
      pageSize: 20,
      totalElements: 1,
      totalPages: 1,
      isLast: true,
    });
    vi.mocked(petApi.getPetById).mockResolvedValue(mockPet);

    renderComponent();

    await waitFor(() => {
      expect(screen.getByText('Milu')).toBeInTheDocument();
    });

    await userEvent.click(screen.getByRole('button', { name: 'Xem chi tiết' }));
    expect(screen.getByText('Chi tiết: Milu')).toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: 'Đóng' }));

    await userEvent.click(screen.getByRole('button', { name: 'Chỉnh sửa' }));
    expect(screen.getByText('Chỉnh sửa hồ sơ thú cưng')).toBeInTheDocument();
  });

  it('opens add pet modal', async () => {
    renderComponent();

    const addButton = await screen.findByRole('button', { name: /Thêm thú cưng mới/i });
    await userEvent.click(addButton);

    expect(screen.getByText('Đăng ký hồ sơ thú cưng')).toBeInTheDocument();

    await waitFor(() => {
      expect(screen.getByRole('option', { name: 'Chó' })).toBeInTheDocument();
    });
  });
});
