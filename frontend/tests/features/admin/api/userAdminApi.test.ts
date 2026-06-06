import { describe, it, expect, vi, beforeEach } from 'vitest';
import { userAdminApi } from '~/features/admin/api/userAdminApi';
import axiosClient from '~/shared/api/axiosClient';
import type { CreateUserRequest, UpdateUserRequest, UserResponse } from '~/types/user';
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

describe('userAdminApi', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  const mockUsersPage: PageResponse<UserResponse> = {
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
    ],
    pageNumber: 1,
    pageSize: 10,
    totalElements: 1,
    totalPages: 1,
    isLast: true,
  };

  it('gets users with pagination and role filter', async () => {
    vi.mocked(axiosClient.get).mockResolvedValue(mockUsersPage.content as any);

    const result = await userAdminApi.getUsers({ page: 1, limit: 10, role: 'ADMIN' });

    expect(axiosClient.get).toHaveBeenCalledWith('/users', {
      params: { page: 1, limit: 10, role: 'ADMIN' },
    });
    expect(result).toEqual(mockUsersPage);
  });

  it('creates user', async () => {
    const mockUser = mockUsersPage.content[0];
    vi.mocked(axiosClient.post).mockResolvedValue(mockUser as any);

    const request: CreateUserRequest = {
      fullName: 'Admin User',
      email: 'admin@pccms.com',
      roleCode: 'ADMIN',
    };
    const result = await userAdminApi.createUser(request);

    expect(axiosClient.post).toHaveBeenCalledWith('/users', request);
    expect(result).toEqual(mockUser);
  });

  it('updates user', async () => {
    const mockUser = mockUsersPage.content[0];
    vi.mocked(axiosClient.put).mockResolvedValue(mockUser as any);

    const request: UpdateUserRequest = { fullName: 'Updated Admin' };
    const result = await userAdminApi.updateUser('u1', request);

    expect(axiosClient.put).toHaveBeenCalledWith('/users/u1', request);
    expect(result).toEqual(mockUser);
  });

  it('locks user', async () => {
    vi.mocked(axiosClient.patch).mockResolvedValue({ data: { data: null } });

    await userAdminApi.lockUser('u1');

    expect(axiosClient.patch).toHaveBeenCalledWith('/users/u1/lock');
  });

  it('disables user', async () => {
    vi.mocked(axiosClient.patch).mockResolvedValue({ data: { data: null } });

    await userAdminApi.disableUser('u1');

    expect(axiosClient.patch).toHaveBeenCalledWith('/users/u1/disable');
  });
});
