import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { BoardingLogPage } from '~/features/reception/pages/BoardingLogPage';
import { boardingApi } from '~/shared/api/boardingApi';

vi.mock('~/shared/api/boardingApi');
vi.mock('react-hot-toast', () => ({
  default: { success: vi.fn(), error: vi.fn() },
}));
vi.mock('~/shared/auth/tokenStorage', () => ({
  hasAccessToken: () => true,
}));
vi.mock('~/features/auth/context/AuthContext', () => ({
  useAuth: () => ({
    isAuthenticated: true,
    user: { id: 'staff-1', roleCode: 'STAFF', fullName: 'Staff' },
  }),
}));

const mockStays = [
  {
    sessionId: 'session-1',
    petId: 'pet-1',
    petName: 'Milu',
    roomLabel: 'STANDARD',
    currentDay: 2,
    totalDays: 5,
    todayLogSummary: 'Đã cập nhật sáng',
  },
];

function renderPage() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return render(
    <QueryClientProvider client={queryClient}>
      <BoardingLogPage />
    </QueryClientProvider>
  );
}

describe('BoardingLogPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(boardingApi.getStaffActiveStays).mockResolvedValue(mockStays);
    vi.mocked(boardingApi.getStaffSessionLogs).mockResolvedValue([]);
    vi.mocked(boardingApi.upsertStaffCareLog).mockResolvedValue({
      id: 'log-1',
      petId: 'pet-1',
      petName: 'Milu',
      logDate: '2026-06-05',
      periodCode: 'MORNING',
      periodLabel: 'Sáng',
      feedingStatus: 'Ăn tốt',
      hygieneStatus: 'Bình thường',
      healthNote: null,
      staffNote: 'Ghi chú',
      mediaCaptions: [],
    });
  });

  it('renders active stays with Vietnamese labels', async () => {
    renderPage();

    await waitFor(() => {
      expect(screen.getAllByText(/STANDARD — Milu/).length).toBeGreaterThan(0);
      expect(screen.getByText(/Ngày 2\/5/)).toBeInTheDocument();
      expect(screen.getByText('Đã cập nhật sáng')).toBeInTheDocument();
    });
  });

  it('saves care log for selected stay', async () => {
    const user = userEvent.setup();
    renderPage();

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Lưu nhật ký' })).toBeInTheDocument();
    });

    await user.click(screen.getByRole('button', { name: 'Lưu nhật ký' }));

    await waitFor(() => {
      expect(boardingApi.upsertStaffCareLog).toHaveBeenCalledWith(
        expect.objectContaining({
          sessionId: 'session-1',
          periodCode: 'MORNING',
        })
      );
    });
  });
});
