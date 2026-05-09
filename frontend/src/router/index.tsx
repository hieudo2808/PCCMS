import { type ReactNode } from "react";
import { createBrowserRouter, Navigate } from "react-router-dom";
import { mockAuth } from "~/constants/auth";
import type { RoleKey } from "~/types/navigation";

// Layouts
import { DashboardLayout } from "~/components/layouts";
import { AuthLayout } from "~/components/layouts/AuthLayout";

// Features
import { LoginPage, RegisterPage, ForgotPasswordPage } from "~/features/auth";
// Owner pages
import {
    OwnerDashboard,
    PetProfilesPage,
    UnifiedBookingPage,
    BoardingTrackingPage,
    BoardingBookingPage,
    PaymentsPage,
    ProfilePage,
} from "~/features/owner";
import { GroomingBookingPage } from "~/features/owner/pages/GroomingBookingPage";
import {
    ReceptionDashboard,
    AppointmentReceptionPage,
    GroomingBoardPage,
    BoardingReceptionPage,
    BoardingLogPage,
    CashierPage,
} from "~/features/reception";
import { DoctorDashboard, DoctorQueuePage, MedicalRecordPage } from "~/features/doctor";
import {
    AdminDashboard,
    AccountsPage,
    CatalogPage,
    RoomsPage,
    SchedulePage,
    ReportsPage,
} from "~/features/admin";

function AuthGuard({ children, requiredRole }: { children: ReactNode; requiredRole: RoleKey }) {
    if (!mockAuth.isAuthenticated) {
        return <Navigate to="/login" replace />;
    }

    if (mockAuth.currentRole !== requiredRole) {
        const fallbackPath =
            mockAuth.currentRole === "public" ? "/login" : `/${mockAuth.currentRole}`;
        return <Navigate to={fallbackPath} replace />;
    }

    return <>{children}</>;
}

export const router = createBrowserRouter([
    {
        path: "/",
        element: (
            <Navigate
                to={mockAuth.currentRole === "public" ? "/login" : `/${mockAuth.currentRole}`}
                replace
            />
        ),
    },

    // Auth routes
    {
        element: <AuthLayout />,
        children: [
            { path: "/login", element: <LoginPage /> },
            { path: "/register", element: <RegisterPage /> },
            { path: "/forgot-password", element: <ForgotPasswordPage /> },
        ],
    },

    // OWNER ROUTES
    {
        path: "/owner",
        element: (
            <AuthGuard requiredRole="owner">
                <DashboardLayout />
            </AuthGuard>
        ),
        children: [
            { index: true, element: <OwnerDashboard /> },
            { path: "pets", element: <PetProfilesPage /> },
            { path: "book", element: <UnifiedBookingPage /> },
            { path: "grooming", element: <GroomingBookingPage /> },
            { path: "boarding/book", element: <BoardingBookingPage /> },
            { path: "boarding/tracking", element: <BoardingTrackingPage /> },
            { path: "payments", element: <PaymentsPage /> },
            { path: "profile", element: <ProfilePage /> },
        ],
    },

    // STAFF ROUTES
    {
        path: "/reception",
        element: (
            <AuthGuard requiredRole="reception">
                <DashboardLayout />
            </AuthGuard>
        ),
        children: [
            { index: true, element: <ReceptionDashboard /> },
            { path: "appointments", element: <AppointmentReceptionPage /> },
            { path: "grooming-board", element: <GroomingBoardPage /> },
            { path: "boarding-reception", element: <BoardingReceptionPage /> },
            { path: "boarding-log", element: <BoardingLogPage /> },
            { path: "cashier", element: <CashierPage /> },
        ],
    },
    {
        path: "/doctor",
        element: (
            <AuthGuard requiredRole="doctor">
                <DashboardLayout />
            </AuthGuard>
        ),
        children: [
            { index: true, element: <DoctorDashboard /> },
            { path: "queue", element: <DoctorQueuePage /> },
            { path: "medical-record", element: <MedicalRecordPage /> },
        ],
    },
    {
        path: "/admin",
        element: (
            <AuthGuard requiredRole="admin">
                <DashboardLayout />
            </AuthGuard>
        ),
        children: [
            { index: true, element: <AdminDashboard /> },
            { path: "accounts", element: <AccountsPage /> },
            { path: "catalog", element: <CatalogPage /> },
            { path: "rooms", element: <RoomsPage /> },
            { path: "schedule", element: <SchedulePage /> },
            { path: "reports", element: <ReportsPage /> },
        ],
    },
]);
