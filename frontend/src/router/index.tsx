import { type ReactNode } from "react";
import { createBrowserRouter, Navigate, Link } from "react-router-dom";
import { useAuth } from "~/features/auth/context/AuthContext";
import type { RoleKey } from "~/types/navigation";
import { recordAuthFailure } from "~/shared/auth/authSession";


// Layouts
import { DashboardLayout } from "~/components/layouts";
import { AuthLayout } from "~/components/layouts/AuthLayout";

// Features
import { LoginPage, RegisterPage, ForgotPasswordPage } from "~/features/auth";
// Owner pages
import {
    OwnerDashboard,
    PetProfilesPage,
    OwnerAppointmentsPage,
    UnifiedBookingPage,
    GroomingBookingPage,
    GroomingTrackingPage,
    BoardingTrackingPage,
    PaymentsPage,
    PetMedicalRecordsPage,
} from "~/features/owner";
import { ProfilePage } from "~/shared/pages/ProfilePage";

import {
    ReceptionDashboard,
    AppointmentReceptionPage,
    GroomingBoardPage,
    BoardingLogPage,
} from "~/features/reception";
import { MySchedulePage as ReceptionMySchedulePage } from "~/features/reception/pages/MySchedulePage";
import { DoctorDashboard, DoctorQueuePage, MedicalRecordPage, MedicalRecordListPage, PrescriptionPage } from "~/features/doctor";

import { MySchedulePage as DoctorMySchedulePage } from "~/features/doctor/pages/MySchedulePage";
import {
    AdminDashboard,
    AccountsPage,
    CatalogPage,
    RoomsPage,
    WorkSchedulePage,
    ShiftChangeRequestPage,
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
        reception: "staff",
        veterinarian: "veterinarian",
        vet: "veterinarian",
        doctor: "veterinarian",
        admin: "admin",
    };
    return aliases[key] ?? "public";
}

function AuthGuard({ children, requiredRole }: { children: ReactNode; requiredRole: RoleKey }) {
    const { user } = useAuth();

    if (!user) {
        return <Navigate to="/login" replace />;
    }

    const currentRole = normalizeRole(user?.roleCode);

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
            { path: "appointments", element: <OwnerAppointmentsPage /> },
            { path: "book", element: <UnifiedBookingPage /> },
            { path: "grooming/book", element: <GroomingBookingPage /> },
            { path: "grooming/tracking", element: <GroomingTrackingPage /> },
            { path: "boarding/tracking", element: <BoardingTrackingPage /> },
            { path: "pets/:petId/medical-records", element: <PetMedicalRecordsPage /> },
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
            { path: "my-schedule", element: <ReceptionMySchedulePage /> },
            { path: "profile", element: <ProfilePage /> },
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
            { path: "medical-records", element: <MedicalRecordListPage /> },
            { path: "medical-records/:id", element: <MedicalRecordPage /> },
            { path: "medical-records/:id/prescriptions", element: <PrescriptionPage /> },
            { path: "medical-records/appointment/:appointmentId", element: <MedicalRecordPage /> },
            { path: "my-schedule", element: <DoctorMySchedulePage /> },
            { path: "profile", element: <ProfilePage /> },
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
            { path: "schedule", element: <WorkSchedulePage /> },
            { path: "shift-requests", element: <ShiftChangeRequestPage /> },
            { path: "reports", element: <ReportsPage /> },
            { path: "profile", element: <ProfilePage /> },
        ],
    },

    // Legacy path redirects
    { path: "/reception", element: <Navigate to="/staff" replace /> },
    { path: "/reception/appointments", element: <Navigate to="/staff/appointments" replace /> },
    { path: "/reception/grooming-board", element: <Navigate to="/staff/grooming-board" replace /> },
    { path: "/reception/boarding-log", element: <Navigate to="/staff/boarding-log" replace /> },
    { path: "/doctor", element: <Navigate to="/veterinarian" replace /> },
    { path: "/doctor/queue", element: <Navigate to="/veterinarian/queue" replace /> },
    { path: "/doctor/medical-record", element: <Navigate to="/veterinarian/medical-records" replace /> },
    { path: "/veterinarian/medical-record", element: <Navigate to="/veterinarian/medical-records" replace /> },


    // Catch-all 404 route
    { 
        path: "*", 
        element: (
            <div className="flex min-h-screen flex-col items-center justify-center bg-slate-50 p-6">
                <h1 className="mb-4 text-6xl font-bold text-slate-900">404</h1>
                <h2 className="mb-2 text-xl font-semibold text-slate-700">Trang không tồn tại</h2>
                <p className="mb-8 max-w-md text-center text-slate-500">
                    Đường dẫn bạn đang truy cập chưa được phát triển hoặc không tồn tại. Vui lòng kiểm tra lại!
                </p>
                <Link to="/" className="rounded-lg bg-primary-600 px-6 py-2.5 font-medium text-white transition hover:bg-primary-700">
                    Về trang chủ
                </Link>
            </div>
        )
    }
]);
