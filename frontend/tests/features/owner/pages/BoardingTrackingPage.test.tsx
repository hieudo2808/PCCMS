import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { BoardingTrackingPage } from '~/features/owner/pages/BoardingTrackingPage';
import { boardingApi } from '~/shared/api/boardingApi';
import { PET_MESSAGES } from '~/features/owner/schema/petSchema';

vi.mock('~/shared/api/boardingApi');
vi.mock('~/shared/auth/tokenStorage', () => ({
  hasAccessToken: () => true,
}));
vi.mock('~/features/auth/context/AuthContext', () => ({
  useAuth: () => ({
    isAuthenticated: true,
    user: { id: '1', roleCode: 'OWNER', fullName: 'Test' },
  }),
}));

const mockStays = [
  { petId: 'pet-1', petName: 'Milu', speciesName: 'Chó', breedName: 'Poodle' },
  { petId: 'pet-2', petName: 'Bông', speciesName: 'Mèo', breedName: 'Anh lông ngắn' },
];

const mockLogs = [
  {
    id: 'log-1',
    petId: 'pet-1',
    petName: 'Milu',
    logDate: '2026-06-05',
    periodCode: 'MORNING',
    periodLabel: 'Sáng',
    feedingStatus: 'Ăn tốt',
    hygieneStatus: 'Bình thường',
    healthNote: null,
    staffNote: 'Ghi chú nhân viên',
    mediaCaptions: [],
  },
  {
    id: 'log-2',
    petId: 'pet-2',
    petName: 'Bông',
    logDate: '2026-06-04',
    periodCode: 'AFTERNOON',
    periodLabel: 'Chiều',
    feedingStatus: 'Ăn vừa đủ',
    hygieneStatus: 'Ổn',
    healthNote: null,
    staffNote: null,
    mediaCaptions: [],
  },
];

function renderPage() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return render(
    <QueryClientProvider client={queryClient}>
      <BoardingTrackingPage />
    </QueryClientProvider>
  );
}

describe('BoardingTrackingPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('shows UC008 empty state when no active stays', async () => {
    vi.mocked(boardingApi.getActiveStays).mockResolvedValue([]);

    renderPage();

    await waitFor(() => {
      expect(screen.getByText(PET_MESSAGES.noBoardingPets)).toBeInTheDocument();
    });
  });

  it('renders stay filters and care logs', async () => {
    vi.mocked(boardingApi.getActiveStays).mockResolvedValue(mockStays);
    vi.mocked(boardingApi.getCareLogs).mockResolvedValue(mockLogs);

    renderPage();

    await waitFor(() => {
      expect(screen.getByText('Milu')).toBeInTheDocument();
      expect(screen.getByText('Bông')).toBeInTheDocument();
    });

    await waitFor(() => {
      expect(screen.getAllByText(/Ăn tốt/).length).toBeGreaterThan(0);
      expect(screen.getByText(/Ghi chú của nhân viên/)).toBeInTheDocument();
    });
  });

  it('filters care logs by pet', async () => {
    const user = userEvent.setup();
    vi.mocked(boardingApi.getActiveStays).mockResolvedValue(mockStays);
    vi.mocked(boardingApi.getCareLogs).mockImplementation(async (petId?: string) => {
      if (petId === 'pet-2') {
        return [mockLogs[1]];
      }
      return mockLogs;
    });

    renderPage();

    await waitFor(() => {
      expect(screen.getByText('Bông')).toBeInTheDocument();
    });

    await user.click(screen.getByRole('button', { name: 'Bông' }));

    await waitFor(() => {
      expect(boardingApi.getCareLogs).toHaveBeenCalledWith('pet-2');
    });
  });

  it('shows care log updating message when stays exist but no logs', async () => {
    vi.mocked(boardingApi.getActiveStays).mockResolvedValue(mockStays);
    vi.mocked(boardingApi.getCareLogs).mockResolvedValue([]);

    renderPage();

    await waitFor(() => {
      expect(screen.getByText('Chưa có nhật ký chăm sóc cho bộ lọc này.')).toBeInTheDocument();
    });
  });
});
