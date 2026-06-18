import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { BrowserRouter } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import toast from "react-hot-toast";
import { RegisterPage } from "~/features/auth/pages/RegisterPage";
import { AuthProvider } from "~/features/auth/context/AuthContext";
import { server } from "@tests/mocks/server";
import { http, HttpResponse } from "msw";

vi.mock("react-hot-toast", () => ({
    default: {
        error: vi.fn(),
        success: vi.fn(),
    },
}));

const originalLocation = window.location;

describe("RegisterPage", () => {
    const renderComponent = () => {
        const queryClient = new QueryClient();
        return render(
            <QueryClientProvider client={queryClient}>
                <AuthProvider>
                    <BrowserRouter>
                        <RegisterPage />
                    </BrowserRouter>
                </AuthProvider>
            </QueryClientProvider>
        );
    };

    beforeEach(() => {
        vi.clearAllMocks();
        // @ts-expect-error test overrides location
        delete window.location;
        // @ts-expect-error test overrides location
        window.location = { ...originalLocation, href: "http://localhost:3000", assign: vi.fn() };
    });

    afterEach(() => {
        // @ts-expect-error restore location
        window.location = originalLocation;
    });

    it("shows validation errors for empty required fields", async () => {
        renderComponent();
        await userEvent.click(screen.getByRole("button", { name: "Tạo tài khoản" }));

        expect(await screen.findByText("Họ và tên không được để trống")).toBeInTheDocument();
        expect(await screen.findByText("Số điện thoại không được để trống")).toBeInTheDocument();
        expect(await screen.findByText("Email không được để trống")).toBeInTheDocument();
        expect(await screen.findByText("Mật khẩu phải có ít nhất 8 ký tự")).toBeInTheDocument();
    });

    it("shows error if passwords do not match", async () => {
        renderComponent();
        await userEvent.type(screen.getByLabelText("Họ tên"), "John Doe");
        await userEvent.type(screen.getByLabelText("Số điện thoại"), "0901234567");
        await userEvent.type(screen.getByLabelText("Email"), "john@test.com");
        await userEvent.type(screen.getByLabelText("Mật khẩu"), "password123");
        await userEvent.type(screen.getByLabelText("Xác nhận mật khẩu"), "password456");
        await userEvent.click(screen.getByRole("button", { name: "Tạo tài khoản" }));

        expect(await screen.findByText("Mật khẩu xác nhận không khớp")).toBeInTheDocument();
    });

    it("registers successfully with phone number", async () => {
        server.use(
            http.post("*/auth/register", async ({ request }) => {
                const body = await request.json() as Record<string, unknown>;
                expect(body.phone).toBe("0901234567");
                expect(body.confirmPassword).toBeUndefined();
                return HttpResponse.json({
                    success: true,
                    code: 201,
                    message: "OK",
                    data: {
                        token: "valid-token",
                        refreshToken: "refresh-token",
                        user: { userId: "1", roleCode: "OWNER", roleName: "Chủ nuôi" },
                    },
                });
            })
        );

        renderComponent();
        await userEvent.type(screen.getByLabelText("Họ tên"), "John Doe");
        await userEvent.type(screen.getByLabelText("Số điện thoại"), "0901234567");
        await userEvent.type(screen.getByLabelText("Email"), "john@test.com");
        await userEvent.type(screen.getByLabelText("Mật khẩu"), "password123");
        await userEvent.type(screen.getByLabelText("Xác nhận mật khẩu"), "password123");
        await userEvent.click(screen.getByRole("button", { name: "Tạo tài khoản" }));

        await waitFor(() => {
            expect(toast.success).toHaveBeenCalledWith("Đăng ký tài khoản thành công");
        });
    });

    it("has type='button' for login navigation to prevent form submission", () => {
        renderComponent();
        const btn = screen.getByRole("button", { name: "Về trang đăng nhập" });
        expect(btn).toHaveAttribute("type", "button");
    });
});
