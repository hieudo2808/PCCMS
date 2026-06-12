import React, { useEffect, useState } from "react";
import { createPortal } from "react-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import toast from "react-hot-toast";
import { Card } from "~/components/molecules/Card";
import { DataTable } from "~/components/molecules/DataTable";
import { Modal } from "~/components/molecules/Modal";
import { Tag } from "~/components/atoms/Tag";
import { Button } from "~/components/atoms/Button";
import { Input } from "~/components/atoms/Input";
import { EmptyState } from "~/components/molecules/EmptyState";
import {
    createAccount,
    resetAccountPassword,
    searchAccounts,
    updateAccount,
    type AccountCredential,
    type AccountPayload,
} from "~/features/admin/account-management/accountService";
import type { Account, AccountRole, AccountStatus } from "~/features/admin/account-management/types";
import { AccountModal, type AccountFormValues } from "./AccountModal";

const ROLE_OPTIONS = [
    { value: "", label: "Tất cả vai trò" },
    { value: "admin", label: "Admin" },
    { value: "doctor", label: "Bác sĩ thú y" },
    { value: "staff", label: "Lễ tân" },
    { value: "owner", label: "Khách hàng" },
];

const roleCodeByRole: Record<AccountRole, string> = {
    admin: "ADMIN",
    doctor: "VETERINARIAN",
    staff: "STAFF",
    owner: "OWNER",
};

function getRoleCode(account: Account) {
    return account.roleCode ?? roleCodeByRole[account.roles[0] ?? "owner"];
}

function toPayload(values: AccountFormValues): AccountPayload {
    return {
        fullName: values.fullName.trim(),
        email: values.email.trim(),
        phone: values.phone.trim(),
        roleCode: values.roleCode,
        statusCode: values.statusCode,
    };
}

function getMenuPosition(button: HTMLButtonElement) {
    const rect = button.getBoundingClientRect();
    const menuWidth = 192;
    return {
        top: rect.bottom + 8,
        left: Math.max(8, Math.min(rect.right - menuWidth, window.innerWidth - menuWidth - 8)),
    };
}

export function AccountsPage() {
    const queryClient = useQueryClient();
    const [page, setPage] = useState(1);
    const [role, setRole] = useState<AccountRole | "">("");
    const [keyword, setKeyword] = useState("");
    const [debouncedKeyword, setDebouncedKeyword] = useState("");
    const [openMenuId, setOpenMenuId] = useState<string | null>(null);
    const [menuPosition, setMenuPosition] = useState({ top: 0, left: 0 });
    const [modalMode, setModalMode] = useState<"create" | "edit" | null>(null);
    const [editingAccount, setEditingAccount] = useState<Account | null>(null);
    const [credentialResult, setCredentialResult] = useState<AccountCredential | null>(null);

    useEffect(() => {
        const timer = window.setTimeout(() => {
            setDebouncedKeyword(keyword);
            setPage(1);
        }, 500);
        return () => window.clearTimeout(timer);
    }, [keyword]);

    useEffect(() => {
        if (!openMenuId) return;

        const close = () => setOpenMenuId(null);
        window.addEventListener("scroll", close, true);
        window.addEventListener("resize", close);
        return () => {
            window.removeEventListener("scroll", close, true);
            window.removeEventListener("resize", close);
        };
    }, [openMenuId]);

    const { data: accounts, isLoading, isError } = useQuery({
        queryKey: ["admin", "accounts", page, role, debouncedKeyword],
        queryFn: () =>
            searchAccounts({
                fullName: debouncedKeyword,
                role: role || undefined,
                status: undefined,
            }),
    });

    const createMutation = useMutation({
        mutationFn: createAccount,
        onSuccess: (result) => {
            toast.success("Tạo tài khoản thành công");
            setModalMode(null);
            setCredentialResult(result);
            queryClient.invalidateQueries({ queryKey: ["admin", "accounts"] });
        },
        onError: () => toast.error("Không thể tạo tài khoản"),
    });

    const updateMutation = useMutation({
        mutationFn: ({ id, payload }: { id: string; payload: AccountPayload }) => updateAccount(id, payload),
        onSuccess: () => {
            toast.success("Cập nhật tài khoản thành công");
            setModalMode(null);
            setEditingAccount(null);
            queryClient.invalidateQueries({ queryKey: ["admin", "accounts"] });
        },
        onError: () => toast.error("Không thể cập nhật tài khoản"),
    });

    const resetPasswordMutation = useMutation({
        mutationFn: resetAccountPassword,
        onSuccess: (result) => {
            toast.success("Đã tạo mật khẩu tạm");
            setCredentialResult(result);
            queryClient.invalidateQueries({ queryKey: ["admin", "accounts"] });
        },
        onError: () => toast.error("Không thể reset mật khẩu"),
    });

    const users = accounts || [];
    const activeMenuAccount = users.find((user) => user.accountId === openMenuId) ?? null;

    const handleRoleChange = (event: React.ChangeEvent<HTMLSelectElement>) => {
        setRole(event.target.value as AccountRole | "");
        setPage(1);
    };

    const toggleMenu = (account: Account, button: HTMLButtonElement) => {
        if (openMenuId === account.accountId) {
            setOpenMenuId(null);
            return;
        }

        setMenuPosition(getMenuPosition(button));
        setOpenMenuId(account.accountId);
    };

    const openCreate = () => {
        setEditingAccount(null);
        setModalMode("create");
    };

    const openEdit = (account: Account) => {
        setOpenMenuId(null);
        setEditingAccount(account);
        setModalMode("edit");
    };

    const submitAccount = (data: AccountFormValues) => {
        const payload = toPayload(data);
        if (modalMode === "edit" && editingAccount) {
            updateMutation.mutate({ id: editingAccount.accountId, payload });
            return;
        }
        createMutation.mutate(payload);
    };

    const renderStatusBadge = (status: AccountStatus) => {
        switch (status) {
            case "active":
                return <Tag tone="green">ACTIVE</Tag>;
            case "locked":
                return <Tag tone="red">LOCKED</Tag>;
            case "disabled":
                return <Tag tone="amber">DISABLED</Tag>;
            default:
                return <Tag tone="default">{status.toUpperCase()}</Tag>;
        }
    };

    const copyTemporaryPassword = async () => {
        if (!credentialResult?.temporaryPassword) return;
        await navigator.clipboard.writeText(credentialResult.temporaryPassword);
        toast.success("Đã copy mật khẩu tạm");
    };

    if (isLoading) {
        return (
            <div className="flex justify-center p-8">
                <div className="h-8 w-8 animate-spin rounded-full border-b-2 border-indigo-600" />
            </div>
        );
    }

    if (isError) return <EmptyState title="Lỗi" description="Lỗi tải danh sách tài khoản" />;

    return (
        <div className="grid gap-6">
            <div className="flex flex-wrap items-center justify-between gap-3">
                <div className="flex w-full max-w-md items-center gap-4">
                    <div className="flex-1">
                        <Input
                            placeholder="Tìm kiếm tài khoản..."
                            value={keyword}
                            onChange={(event) => setKeyword(event.target.value)}
                        />
                    </div>
                    <select
                        className="h-[42px] rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm text-slate-700 outline-none focus:border-primary-500 focus:ring-2 focus:ring-primary-500/20"
                        value={role}
                        onChange={handleRoleChange}
                    >
                        {ROLE_OPTIONS.map((opt) => (
                            <option key={opt.value} value={opt.value}>
                                {opt.label}
                            </option>
                        ))}
                    </select>
                </div>
                <Button onClick={openCreate}>Thêm tài khoản</Button>
            </div>

            <Card title="Quản lý tài khoản">
                {users.length === 0 ? (
                    <EmptyState title="Trống" description="Không có tài khoản nào" />
                ) : (
                    <DataTable
                        columns={["Họ tên", "Email", "Vai trò", "Trạng thái", "Hành động"]}
                        rows={users.map((user) => [
                            user.fullName,
                            user.email,
                            user.roleName || user.roleCode,
                            renderStatusBadge(user.status),
                            <Button
                                key={user.accountId}
                                variant="outline"
                                className="h-auto px-2 py-1 text-xs"
                                onClick={(event) => toggleMenu(user, event.currentTarget)}
                                aria-label={`Thao tác ${user.fullName}`}
                            >
                                ...
                            </Button>,
                        ])}
                    />
                )}
            </Card>

            {openMenuId && activeMenuAccount && createPortal(
                <>
                    <button
                        type="button"
                        className="fixed inset-0 z-[900] cursor-default bg-transparent"
                        aria-label="Đóng menu thao tác"
                        onClick={() => setOpenMenuId(null)}
                    />
                    <div
                        className="fixed z-[901] w-48 rounded-xl border border-slate-200 bg-white py-1 shadow-xl"
                        style={{ top: menuPosition.top, left: menuPosition.left }}
                        role="menu"
                    >
                        <button
                            role="menuitem"
                            className="w-full px-4 py-2 text-left text-sm text-slate-700 hover:bg-slate-100"
                            onClick={() => openEdit(activeMenuAccount)}
                        >
                            Sửa
                        </button>
                        <button
                            role="menuitem"
                            className="w-full px-4 py-2 text-left text-sm text-slate-700 hover:bg-slate-100"
                            disabled={resetPasswordMutation.isPending}
                            onClick={() => {
                                setOpenMenuId(null);
                                resetPasswordMutation.mutate(activeMenuAccount.accountId);
                            }}
                        >
                            Reset mật khẩu
                        </button>
                    </div>
                </>,
                document.body
            )}

            <AccountModal
                isOpen={modalMode !== null}
                mode={modalMode ?? "create"}
                initialValue={
                    editingAccount
                        ? {
                              fullName: editingAccount.fullName,
                              email: editingAccount.email,
                              phone: editingAccount.phone,
                              roleCode: getRoleCode(editingAccount),
                              statusCode:
                                  editingAccount.status === "active"
                                      ? "ACTIVE"
                                      : editingAccount.status === "locked"
                                        ? "LOCKED"
                                        : editingAccount.status === "disabled"
                                          ? "DISABLED"
                                          : "UNVERIFIED",
                          }
                        : undefined
                }
                onClose={() => {
                    setModalMode(null);
                    setEditingAccount(null);
                }}
                onSubmit={submitAccount}
                isSubmitting={createMutation.isPending || updateMutation.isPending}
            />

            <Modal
                isOpen={credentialResult !== null}
                onClose={() => setCredentialResult(null)}
                title="Mật khẩu tạm thời"
            >
                {credentialResult && (
                    <div className="space-y-4">
                        <div className="rounded-xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-800">
                            Mật khẩu tạm chỉ hiển thị một lần. Hãy copy và gửi cho người dùng qua kênh phù hợp.
                        </div>
                        <div className="grid gap-3 rounded-2xl border border-slate-200 bg-slate-50 p-4 text-sm">
                            <div>
                                <p className="text-slate-500">Tài khoản</p>
                                <p className="font-semibold text-slate-900">{credentialResult.account.email}</p>
                            </div>
                            <div>
                                <p className="text-slate-500">Mật khẩu tạm</p>
                                <code className="mt-1 block rounded-xl bg-white px-3 py-2 font-mono text-base font-semibold text-slate-900">
                                    {credentialResult.temporaryPassword}
                                </code>
                            </div>
                            <p className="text-slate-500">
                                Email tự động: {credentialResult.emailSent ? "đã yêu cầu gửi" : "không xác định"}
                            </p>
                        </div>
                        <div className="flex justify-end gap-3">
                            <Button variant="outline" onClick={() => setCredentialResult(null)}>
                                Đóng
                            </Button>
                            <Button onClick={() => void copyTemporaryPassword()}>
                                Copy mật khẩu
                            </Button>
                        </div>
                    </div>
                )}
            </Modal>
        </div>
    );
}
