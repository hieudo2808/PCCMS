import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { LoginPage } from "~/features/auth/pages/LoginPage";
import { BrowserRouter } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { AuthProvider } from "~/features/auth/context/AuthContext";
import { server } from "@tests/mocks/server";
import { http, HttpResponse } from "msw";
import toast from "react-hot-toast";

import { authApi } from "~/features/auth/api/authApi";

vi.mock("react-hot-toast", () => ({
    default: {
        error: vi.fn(),
        success: vi.fn(),
    },
}));

// Mock window.location
const originalLocation = window.location;

describe("LoginPage", () => {
    const queryClient = new QueryClient();

    const renderComponent = () => {
        return render(
            <QueryClientProvider client={queryClient}>
                <AuthProvider>
                    <BrowserRouter>
                        <LoginPage />
                    </BrowserRouter>
                </AuthProvider>
            </QueryClientProvider>
        );
    };

    beforeEach(() => {
        vi.clearAllMocks();
        // @ts-ignore
        delete window.location;
        // @ts-ignore
        window.location = { ...originalLocation, href: "http://localhost:3000", assign: vi.fn() };
    });

    afterEach(() => {
        // @ts-ignore
        window.location = originalLocation;
    });

    it("renders login form", () => {
        renderComponent();
        expect(screen.getByRole("heading", { name: "Đăng nhập" })).toBeInTheDocument();
        expect(screen.getByLabelText(/Email/i)).toBeInTheDocument();
        expect(screen.getByLabelText(/Mật khẩu/i)).toBeInTheDocument();
    });

    it("shows validation errors if fields are empty", async () => {
        renderComponent();
        await userEvent.click(screen.getByRole("button", { name: "Đăng nhập" }));
        expect(await screen.findByText("Email không được để trống")).toBeInTheDocument();
        expect(await screen.findByText("Mật khẩu không được để trống")).toBeInTheDocument();
    });

    it("logs in successfully and redirects", async () => {
        server.use(
            http.post("*/auth/login", () => {
                return HttpResponse.json({
                    success: true,
                    code: 200,
                    message: "OK",
                    data: {
                        token: "valid-token",
                        refreshToken: "refresh-token",
                        user: { userId: "1", roleName: "CUSTOMER" },
                    },
                });
            })
        );

        const spy = vi.spyOn(authApi, "login");

        renderComponent();
        await userEvent.type(screen.getByLabelText(/Email/i), "test@example.com");
        await userEvent.type(screen.getByLabelText(/Mật khẩu/i), "password123");
        await userEvent.click(screen.getByRole("button", { name: "Đăng nhập" }));

        await waitFor(
            () => {
                expect(spy).toHaveBeenCalled();
            },
            { timeout: 3000 }
        );

        await waitFor(
            () => {
                expect(toast.success).toHaveBeenCalledWith("Đăng nhập thành công!");
            },
            { timeout: 3000 }
        );
    });

    it("shows error toast on invalid credentials", async () => {
        server.use(
            http.post("*/auth/login", () => {
                return HttpResponse.json(
                    {
                        success: false,
                        code: 401,
                        message: "Sai tài khoản hoặc mật khẩu",
                    },
                    { status: 401 }
                );
            })
        );

        renderComponent();
        await userEvent.type(screen.getByLabelText(/Email/i), "test@example.com");
        await userEvent.type(screen.getByLabelText(/Mật khẩu/i), "wrong");
        await userEvent.click(screen.getByRole("button", { name: "Đăng nhập" }));

        await waitFor(() => {
            expect(toast.error).toHaveBeenCalledWith("Sai tài khoản hoặc mật khẩu");
        });
    });
});
