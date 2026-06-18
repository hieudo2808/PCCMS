import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { AccountsPage } from "~/features/admin/pages/AccountsPage";
import * as accountService from "~/features/admin/account-management/accountService";
import type { Account } from "~/features/admin/account-management/types";

vi.mock("~/features/admin/pages/AccountModal", async () => {
    const React = await import("react");
    return {
        AccountModal: ({ isOpen, onClose, onSubmit, mode }: any) => {
            if (!isOpen) return null;
            return React.createElement("div", { role: "dialog" },
                React.createElement("span", null, mode === "edit" ? "Sửa tài khoản" : "Thêm tài khoản mới"),
                React.createElement("button", {
                    "data-testid": "modal-submit",
                    onClick: () => onSubmit({
                        fullName: "New User",
                        email: "new@pccms.com",
                        phone: "0111222333",
                        roleCode: "STAFF",
                        statusCode: "DISABLED",
                    }),
                }, "Lưu"),
                React.createElement("button", { onClick: onClose }, "Hủy"),
            );
        },
    };
});

vi.mock("~/features/admin/account-management/accountService", () => ({
    searchAccounts: vi.fn(),
    updateAccountRoles: vi.fn(),
    createAccount: vi.fn(),
    updateAccount: vi.fn(),
    resetAccountPassword: vi.fn(),
}));

describe("AccountsPage", () => {
    let queryClient: QueryClient;

    const mockAccounts: Account[] = [
        {
            accountId: "u1",
            fullName: "Admin User",
            email: "admin@pccms.com",
            phone: "0123456789",
            roleCode: "ADMIN",
            roles: ["admin"],
            createdAt: "2026-01-01",
            status: "active",
        },
        {
            accountId: "u2",
            fullName: "Customer User",
            email: "customer@pccms.com",
            phone: "0987654321",
            roleCode: "OWNER",
            roles: ["owner"],
            createdAt: "2026-01-02",
            status: "locked",
        },
    ];

    beforeEach(() => {
        vi.clearAllMocks();
        queryClient = new QueryClient({
            defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
        });
        vi.mocked(accountService.searchAccounts).mockResolvedValue(mockAccounts);
        vi.mocked(accountService.updateAccount).mockResolvedValue(mockAccounts[1]);
        vi.mocked(accountService.resetAccountPassword).mockResolvedValue({
            account: mockAccounts[1],
            temporaryPassword: "Tmp12345!",
            emailSent: true,
        });
        vi.mocked(accountService.createAccount).mockResolvedValue({
            account: mockAccounts[1],
            temporaryPassword: "Tmp12345!",
            emailSent: true,
        });
    });

    const renderComponent = () => {
        render(
            <QueryClientProvider client={queryClient}>
                <AccountsPage />
            </QueryClientProvider>
        );
    };

    it("renders users and disables admin row actions", async () => {
        renderComponent();

        await waitFor(() => {
            expect(screen.getByText("Quản lý tài khoản")).toBeInTheDocument();
        });

        expect(screen.getByRole("button", { name: /Thêm tài khoản/i })).toBeInTheDocument();
        expect(screen.getByPlaceholderText(/Tìm kiếm tài khoản/i)).toBeInTheDocument();
        expect(screen.getByText("Admin User")).toBeInTheDocument();
        expect(screen.getByText("Customer User")).toBeInTheDocument();
        expect(screen.getByRole("button", { name: /Admin User/ })).toBeDisabled();
        expect(screen.getByRole("button", { name: /Customer User/ })).toBeEnabled();
    });

    it("filters by search text", async () => {
        renderComponent();

        const searchInput = await screen.findByPlaceholderText(/Tìm kiếm tài khoản/i);
        await userEvent.type(searchInput, "Admin");

        await waitFor(() => {
            expect(accountService.searchAccounts).toHaveBeenCalledWith(
                expect.objectContaining({ fullName: "Admin" })
            );
        }, { timeout: 1000 });
    });

    it("updates non-admin account status through the edit modal", async () => {
        renderComponent();

        await screen.findByText("Customer User");

        await userEvent.click(screen.getByRole("button", { name: /Customer User/ }));
        await userEvent.click(await screen.findByRole("menuitem", { name: "Sửa" }));
        await userEvent.click(await screen.findByTestId("modal-submit"));

        await waitFor(() => {
            expect(vi.mocked(accountService.updateAccount)).toHaveBeenCalledWith("u2", {
                fullName: "New User",
                email: "new@pccms.com",
                phone: "0111222333",
                roleCode: "STAFF",
                statusCode: "DISABLED",
            });
        });
    });

    it("opens edit modal from non-admin action menu", async () => {
        renderComponent();

        await screen.findByText("Customer User");

        await userEvent.click(screen.getByRole("button", { name: /Customer User/ }));
        await userEvent.click(await screen.findByRole("menuitem", { name: "Sửa" }));
        expect(await screen.findByRole("dialog")).toHaveTextContent("Sửa tài khoản");
    });

    it("shows modal dialog when add button is clicked", async () => {
        renderComponent();

        await screen.findByText("Admin User");
        expect(screen.queryByRole("dialog")).not.toBeInTheDocument();

        await userEvent.click(screen.getByRole("button", { name: /Thêm tài khoản/i }));

        const dialog = await screen.findByRole("dialog");
        expect(dialog).toBeInTheDocument();
        expect(screen.getByText("Thêm tài khoản mới")).toBeInTheDocument();
    });
});
