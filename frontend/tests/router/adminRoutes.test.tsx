import { render, screen } from "@testing-library/react";
import type { ReactNode } from "react";
import { RouterProvider } from "react-router-dom";
import { describe, expect, it, vi } from "vitest";

vi.mock("~/features/auth/context/AuthContext", () => ({
    useAuth: () => ({
        user: { id: "admin-1", roleCode: "ADMIN", fullName: "Admin User" },
        logout: vi.fn(),
    }),
    AuthProvider: ({ children }: { children: ReactNode }) => <>{children}</>,
}));

vi.mock("~/features/admin", () => ({
    AdminDashboard: () => <div>Admin Dashboard Route</div>,
    AccountsPage: () => <div>Admin Accounts Route</div>,
    CatalogPage: () => <div>Admin Catalog Route</div>,
    RoomsPage: () => <div>Admin Rooms Route</div>,
    WorkSchedulePage: () => <div>Admin Schedule Route</div>,
    ShiftChangeRequestPage: () => <div>Admin Shift Requests Route</div>,
    ReportsPage: () => <div>Admin Reports Route</div>,
}));

import { router } from "~/router";

describe("admin routes", () => {
    it("renders rooms page at /admin/rooms", async () => {
        await router.navigate("/admin/rooms");

        render(<RouterProvider router={router} />);

        expect(await screen.findByText("Admin Rooms Route")).toBeInTheDocument();
        expect(screen.queryByText("404")).not.toBeInTheDocument();
    });
});
