import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AccountsPage } from '~/features/admin/pages/AccountsPage';
import { userAdminApi } from '~/features/admin/api/userAdminApi';
import type { PageResponse } from '~/types/api';
import type { UserResponse } from '~/types/user';

vi.mock('~/features/admin/api/userAdminApi', () => ({
  userAdminApi: {
    getUsers: vi.fn(),
    lockUser: vi.fn(),
    disableUser: vi.fn(),
  },
}));

describe('AccountsPage', () => {
  let queryClient: QueryClient;

  beforeEach(() => {
    vi.clearAllMocks();
    queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    });
  });

  const renderComponent = () => {
    render(
      <QueryClientProvider client={queryClient}>
        <AccountsPage />
      </QueryClientProvider>
    );
  };

  const mockUsers: PageResponse<UserResponse> = {
    content: [
      {
        id: 'u1',
        fullName: 'Admin User',
        email: 'admin@pccms.com',
        phone: '0123456789',
        roleCode: 'ADMIN',
        createdAt: '2026-01-01',
        statusCode: 'ACTIVE',
      },
      {
        id: 'u2',
        fullName: 'Customer User',
        email: 'customer@pccms.com',
        phone: '0987654321',
        roleCode: 'OWNER',
        createdAt: '2026-01-02',
        statusCode: 'LOCKED',
      },
    ],
    pageNumber: 1,
    pageSize: 10,
    totalElements: 2,
    totalPages: 1,
    isLast: true,
  };

  it('renders users and correct badge colors', async () => {
    vi.mocked(userAdminApi.getUsers).mockResolvedValue(mockUsers);

    renderComponent();

    await waitFor(() => {
      expect(screen.getByText('Quản lý tài khoản')).toBeInTheDocument();
    });

    await waitFor(() => {
      expect(screen.getByText('Admin User')).toBeInTheDocument();
      expect(screen.getByText('Customer User')).toBeInTheDocument();
    });

    const activeBadge = screen.getByText('ACTIVE');
    expect(activeBadge).toHaveClass('bg-emerald-50', 'text-emerald-700');

    const lockedBadge = screen.getByText('LOCKED');
    expect(lockedBadge).toHaveClass('bg-rose-50', 'text-rose-700');
  });

  it('filters by role', async () => {
    vi.mocked(userAdminApi.getUsers).mockResolvedValue(mockUsers);

    renderComponent();

    await waitFor(() => {
      expect(screen.getByText('Admin User')).toBeInTheDocument();
    });

    const roleSelect = screen.getByRole('combobox');
    await userEvent.selectOptions(roleSelect, 'OWNER');

    await waitFor(() => {
      expect(userAdminApi.getUsers).toHaveBeenCalledWith({ page: 1, limit: 10, role: 'OWNER' });
    });
  });

  it('locks a user', async () => {
    vi.mocked(userAdminApi.getUsers).mockResolvedValue(mockUsers);
    vi.mocked(userAdminApi.lockUser).mockResolvedValue();

    renderComponent();

    await waitFor(() => {
      expect(screen.getByText('Admin User')).toBeInTheDocument();
    });

    const lockButtons = screen.getAllByRole('button', { name: /Khóa/i });
    await userEvent.click(lockButtons[0]);

    await waitFor(() => {
      expect(vi.mocked(userAdminApi.lockUser).mock.calls[0][0]).toBe('u1');
      expect(userAdminApi.getUsers).toHaveBeenCalledTimes(2); // refetch
    });
  });
});
