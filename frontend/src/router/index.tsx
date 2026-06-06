import { type ReactNode } from "react";
import { createBrowserRouter, Navigate, useLocation } from "react-router-dom";
import { useAuth } from "~/features/auth/context/AuthContext";
import type { RoleKey } from "~/types/navigation";
import { recordAuthFailure } from "~/shared/auth/authSession";
import { hasAccessToken } from "~/shared/auth/tokenStorage";

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
    OwnerAppointmentsPage,
    BoardingTrackingPage,
    PaymentsPage,
    ProfilePage,
} from "~/features/owner";
import {
    ReceptionDashboard,
    AppointmentReceptionPage,
    GroomingBoardPage,
    BoardingLogPage,
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

function normalizeRole(roleCode?: string): RoleKey {
    if (!roleCode) return "public";
    const key = roleCode.toLowerCase();
    const aliases: Record<string, RoleKey> = {
        owner: "owner",
        customer: "owner",
        staff: "staff",
        receptionist: "staff",
        veterinarian: "veterinarian",
        admin: "admin",
    };
    return aliases[key] ?? "public";
}

function AuthGuard({ children, requiredRole }: { children: ReactNode; requiredRole: RoleKey }) {
    const { user, isInitializing } = useAuth();
    const location = useLocation();

    if (isInitializing) {
        return (
            <div className="flex min-h-[40vh] items-center justify-center text-sm text-slate-600">
                Đang xác thực quyền truy cập…
            </div>
        );
    }

    if (!user || !hasAccessToken()) {
        recordAuthFailure({
            source: "auth-guard",
            message: !hasAccessToken()
                ? `Không có token khi vào ${location.pathname} — đăng nhập trước`
                : `Không có user trong context khi vào ${location.pathname}`,
        });
        return <Navigate to="/login" replace state={{ from: location.pathname }} />;
    }

    const currentRole = normalizeRole(user.roleCode);

    if (currentRole !== requiredRole) {
        recordAuthFailure({
            source: "auth-guard",
            message: `Role ${user.roleCode} (${currentRole}) không khớp ${requiredRole} tại ${location.pathname}`,
        });
        const fallbackPath = currentRole === "public" ? "/login" : `/${currentRole}`;
        return <Navigate to={fallbackPath} replace />;
    }

    return <>{children}</>;
}

function RootRedirect() {
    const { user } = useAuth();
    const currentRole = normalizeRole(user?.roleCode);
    return <Navigate to={currentRole === "public" ? "/login" : `/${currentRole}`} replace />;
}

export const router = createBrowserRouter([
    {
        path: "/",
        element: <RootRedirect />,
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
            { path: "appointments", element: <OwnerAppointmentsPage /> },
            { path: "boarding/tracking", element: <BoardingTrackingPage /> },
            { path: "payments", element: <PaymentsPage /> },
            { path: "profile", element: <ProfilePage /> },
        ],
    },

    // STAFF ROUTES
    {
        path: "/staff",
        element: (
            <AuthGuard requiredRole="staff">
                <DashboardLayout />
            </AuthGuard>
        ),
        children: [
            { index: true, element: <ReceptionDashboard /> },
            { path: "appointments", element: <AppointmentReceptionPage /> },
            { path: "grooming-board", element: <GroomingBoardPage /> },
            { path: "boarding-log", element: <BoardingLogPage /> },
        ],
    },
    {
        path: "/veterinarian",
        element: (
            <AuthGuard requiredRole="veterinarian">
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

    // Legacy path redirects
    { path: "/reception", element: <Navigate to="/staff" replace /> },
    { path: "/reception/appointments", element: <Navigate to="/staff/appointments" replace /> },
    { path: "/reception/grooming-board", element: <Navigate to="/staff/grooming-board" replace /> },
    { path: "/reception/boarding-log", element: <Navigate to="/staff/boarding-log" replace /> },
    { path: "/doctor", element: <Navigate to="/veterinarian" replace /> },
    { path: "/doctor/queue", element: <Navigate to="/veterinarian/queue" replace /> },
    { path: "/doctor/medical-record", element: <Navigate to="/veterinarian/medical-record" replace /> },
]);
