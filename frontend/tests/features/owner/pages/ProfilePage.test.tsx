import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { ProfilePage } from "~/shared/pages/ProfilePage";

import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { http, HttpResponse } from "msw";
import { server } from "@tests/mocks/server";

const mockUser = {
    id: "123",
    fullName: "John Doe",
    email: "john@example.com",
    phone: "0912345678",
    roleCode: "OWNER",
    createdAt: "2023-01-01",
    statusCode: "ACTIVE",
};

let queryClient: QueryClient;

const renderComponent = () => {
    queryClient = new QueryClient({
        defaultOptions: { queries: { retry: false } },
    });
    return render(
        <QueryClientProvider client={queryClient}>
            <ProfilePage />
        </QueryClientProvider>
    );
};

describe("ProfilePage", () => {
    beforeEach(() => {});

    afterEach(() => {
        vi.clearAllMocks();
    });

    it("renders skeleton while loading", async () => {
        server.use(
            http.get("http://localhost:8080/api/v1/users/me", () => {
                return new Promise(() => {}); // never resolves to simulate loading
            })
        );

        renderComponent();
        expect(screen.getByTestId("profile-skeleton")).toBeInTheDocument();
    });

    it("renders profile data when fetch succeeds", async () => {
        server.use(
            http.get("http://localhost:8080/api/v1/users/me", () => {
                return HttpResponse.json({
                    success: true,
                    code: 200,
                    data: mockUser,
                });
            })
        );

        renderComponent();

        await waitFor(() => {
            expect(screen.queryByTestId("profile-skeleton")).not.toBeInTheDocument();
        });

        expect(screen.getByDisplayValue("John Doe")).toBeInTheDocument();
        expect(screen.getAllByText("john@example.com")[0]).toBeInTheDocument();
    });

    it("updates profile successfully", async () => {
        server.use(
            http.get("http://localhost:8080/api/v1/users/me", () => {
                return HttpResponse.json({
                    success: true,
                    code: 200,
                    data: mockUser,
                });
            }),
            http.put("http://localhost:8080/api/v1/users/me", () => {
                return HttpResponse.json({
                    success: true,
                    code: 200,
                    data: { ...mockUser, fullName: "Jane Doe" },
                });
            })
        );

        renderComponent();

        await waitFor(() => {
            expect(screen.queryByTestId("profile-skeleton")).not.toBeInTheDocument();
        });

        const nameInput = screen.getByDisplayValue("John Doe");
        await userEvent.clear(nameInput);
        await userEvent.type(nameInput, "Jane Doe");

        const saveBtn = screen.getByRole("button", { name: /Lưu hồ sơ/i });
        await userEvent.click(saveBtn);

        await waitFor(() => {
            expect(screen.getByDisplayValue("Jane Doe")).toBeInTheDocument();
        });
    });

    it("shows error if name is empty", async () => {
        server.use(
            http.get("http://localhost:8080/api/v1/users/me", () => {
                return HttpResponse.json({
                    success: true,
                    code: 200,
                    data: mockUser,
                });
            })
        );

        renderComponent();

        await waitFor(() => {
            expect(screen.queryByTestId("profile-skeleton")).not.toBeInTheDocument();
        });

        const nameInput = screen.getByDisplayValue("John Doe");
        await userEvent.clear(nameInput);

        const saveBtn = screen.getByRole("button", { name: /Lưu hồ sơ/i });
        await userEvent.click(saveBtn);

        expect(await screen.findByText("Họ tên không được để trống")).toBeInTheDocument();
    });
});
