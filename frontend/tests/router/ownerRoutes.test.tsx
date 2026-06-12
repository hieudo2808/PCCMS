import { render, screen } from "@testing-library/react";
import type { ReactNode } from "react";
import { RouterProvider } from "react-router-dom";
import { describe, expect, it, vi } from "vitest";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";

vi.mock("~/features/auth/context/AuthContext", () => ({
    useAuth: () => ({
        user: { id: "owner-1", roleCode: "CUSTOMER", fullName: "Owner User" },
        logout: vi.fn(),
    }),
    AuthProvider: ({ children }: { children: ReactNode }) => <>{children}</>,
}));

vi.mock("~/features/owner", () => ({
    OwnerDashboard: () => <div>Owner Dashboard Route</div>,
    PetProfilesPage: () => <div>Owner Pets Route</div>,
    OwnerAppointmentsPage: () => <div>Owner Appointments Route</div>,
    UnifiedBookingPage: () => <div>Owner Booking Route</div>,
    GroomingBookingPage: () => <div>Owner Grooming Booking Route</div>,
    GroomingTrackingPage: () => <div>Owner Grooming Tracking Route</div>,
    BoardingTrackingPage: () => <div>Owner Boarding Tracking Route</div>,
    PaymentsPage: () => <div>Owner Payments Route</div>,
}));

import { router } from "~/router";

describe("owner routes", () => {
    it("allows CUSTOMER role accounts to access pet profiles", async () => {
        await router.navigate("/owner/pets");

        render(
            <QueryClientProvider client={new QueryClient()}>
                <RouterProvider router={router} />
            </QueryClientProvider>
        );

        expect(await screen.findByText("Owner Pets Route")).toBeInTheDocument();
        expect(screen.queryByText("404")).not.toBeInTheDocument();
    });

    it("renders owner appointments route", async () => {
        await router.navigate("/owner/appointments");

        render(
            <QueryClientProvider client={new QueryClient()}>
                <RouterProvider router={router} />
            </QueryClientProvider>
        );

        expect(await screen.findByText("Owner Appointments Route")).toBeInTheDocument();
        expect(screen.queryByText("404")).not.toBeInTheDocument();
    });
});
