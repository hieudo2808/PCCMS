import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import { userAdminApi } from '../api/userAdminApi';
import { Card } from '~/components/molecules/Card';
import { DataTable } from '~/components/molecules/DataTable';
import { Tag } from '~/components/atoms/Tag';
import { Button } from '~/components/atoms/Button';
import { EmptyState } from '~/components/molecules/EmptyState';
import type { UserRole } from '~/types/user';

const ROLE_OPTIONS = [
  { value: '', label: 'Tất cả vai trò' },
  { value: 'ADMIN', label: 'Admin' },
  { value: 'VETERINARIAN', label: 'Bác sĩ thú y' },
  { value: 'STAFF', label: 'Lễ tân' },
  { value: 'OWNER', label: 'Khách hàng' },
];

export function AccountsPage() {
  const queryClient = useQueryClient();
  const [page, setPage] = useState(1);
  const [role, setRole] = useState<UserRole | ''>('');

  const { data, isLoading, isError } = useQuery({
    queryKey: ['admin', 'users', page, role],
    queryFn: () => userAdminApi.getUsers({ page, limit: 10, role: role || undefined }),
  });

  const lockMutation = useMutation({
    mutationFn: userAdminApi.lockUser,
    onSuccess: () => {
      toast.success('Đã khóa tài khoản');
      queryClient.invalidateQueries({ queryKey: ['admin', 'users'] });
    },
    onError: () => toast.error('Không thể khóa tài khoản'),
  });

  const disableMutation = useMutation({
    mutationFn: userAdminApi.disableUser,
    onSuccess: () => {
      toast.success('Đã vô hiệu hóa tài khoản');
      queryClient.invalidateQueries({ queryKey: ['admin', 'users'] });
    },
    onError: () => toast.error('Không thể vô hiệu hóa tài khoản'),
  });

  const handleRoleChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    setRole(e.target.value as UserRole | '');
    setPage(1);
  };

  const renderStatusBadge = (status: string) => {
    switch (status) {
      case 'ACTIVE':
        return <Tag tone="green">ACTIVE</Tag>;
      case 'LOCKED':
        return <Tag tone="red">LOCKED</Tag>;
      case 'DISABLED':
        return <Tag tone="amber">DISABLED</Tag>;
      default:
        return <Tag tone="default">{status}</Tag>;
    }
  };

  if (isLoading) {
    return (
      <div className="flex justify-center p-8">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-indigo-600"></div>
      </div>
    );
  }

  if (isError) return <EmptyState title="Lỗi" description="Lỗi tải danh sách tài khoản" />;

  const users = data?.content || [];

  return (
    <div className="grid gap-6">
      <Card
        title="Quản lý tài khoản"
        right={
          <select
            className="rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm text-slate-700 outline-none focus:border-primary-500 focus:ring-2 focus:ring-primary-500/20"
            value={role}
            onChange={handleRoleChange}
          >
            {ROLE_OPTIONS.map((opt) => (
              <option key={opt.value} value={opt.value}>
                {opt.label}
              </option>
            ))}
          </select>
        }
      >
        {users.length === 0 ? (
          <EmptyState title="Trống" description="Không có tài khoản nào" />
        ) : (
          <DataTable
            columns={['Họ tên', 'Email', 'Vai trò', 'Trạng thái', 'Hành động']}
            rows={users.map((user) => [
              user.fullName,
              user.email,
              user.roleCode,
              renderStatusBadge(user.statusCode),
              <div key={user.id} className="flex gap-2">
                {user.statusCode !== 'LOCKED' && (
                  <Button
                    variant="outline"
                    className="px-2 py-1 h-auto text-xs"
                    onClick={() => lockMutation.mutate(user.id)}
                    disabled={lockMutation.isPending}
                  >
                    Khóa
                  </Button>
                )}
                {user.statusCode !== 'DISABLED' && (
                  <Button
                    variant="outline"
                    className="px-2 py-1 h-auto text-xs"
                    onClick={() => disableMutation.mutate(user.id)}
                    disabled={disableMutation.isPending}
                  >
                    Vô hiệu hóa
                  </Button>
                )}
              </div>,
            ])}
          />
        )}
      </Card>
    </div>
  );
}
