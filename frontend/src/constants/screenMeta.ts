import {
    Lock,
    User,
    ShieldCheck,
    Home,
    PawPrint,
    Camera,
    CreditCard,
    ClipboardList,
    Sparkles,
    FileText,
    HeartPulse,
    Users,
    Pill,
    Warehouse,
    CalendarDays,
} from "lucide-react";
import type { ScreenKey, ScreenMetaItem } from "~/types/navigation";

export const screenMeta: Record<ScreenKey, ScreenMetaItem> = {
    login: { label: "Đăng nhập", icon: Lock, path: "/login" },
    register: { label: "Đăng ký", icon: User, path: "/register" },
    forgot: { label: "Quên mật khẩu", icon: ShieldCheck, path: "/forgot-password" },

    ownerDashboard: { label: "Dashboard", icon: Home, path: "/owner" },
    pets: { label: "Hồ sơ thú cưng", icon: PawPrint, path: "/owner/pets" },
    ownerAppointments: { label: "Lịch hẹn của tôi", icon: CalendarDays, path: "/owner/appointments" },
    unifiedBooking: { label: "Đặt phòng lưu trú", icon: Warehouse, path: "/owner/book" },
    groomingBooking: { label: "Đăng ký làm đẹp", icon: Sparkles, path: "/owner/grooming/book" },
    groomingTracking: {
        label: "Theo dõi làm đẹp",
        icon: ClipboardList,
        path: "/owner/grooming/tracking",
    },
    boardingTracking: { label: "Theo dõi lưu trú", icon: Camera, path: "/owner/boarding/tracking" },
    payments: { label: "Lịch sử thanh toán", icon: CreditCard, path: "/owner/payments" },
    profile: { label: "Thông tin cá nhân", icon: User, path: "/owner/profile" },

    receptionDashboard: { label: "Dashboard nhân viên", icon: Home, path: "/staff" },
    appointmentReception: {
        label: "Tiếp nhận hẹn khám",
        icon: ClipboardList,
        path: "/staff/appointments",
    },
    groomingBoard: { label: "Kanban dịch vụ spa", icon: Sparkles, path: "/staff/grooming-board" },
    boardingLog: { label: "Nhật ký lưu trú", icon: FileText, path: "/staff/boarding-log" },

    doctorDashboard: { label: "Dashboard bác sĩ", icon: Home, path: "/veterinarian" },
    doctorQueue: { label: "Danh sách chờ khám", icon: HeartPulse, path: "/veterinarian/queue" },
    medicalRecord: {
        label: "Bệnh án và kê đơn",
        icon: FileText,
        path: "/veterinarian/medical-records",
    },


    adminDashboard: { label: "Dashboard quản trị", icon: Home, path: "/admin" },
    accounts: { label: "Quản lý tài khoản", icon: Users, path: "/admin/accounts" },
    catalog: { label: "Danh mục dịch vụ và thuốc", icon: Pill, path: "/admin/catalog" },
    rooms: { label: "Quản lý phòng lưu trú", icon: Warehouse, path: "/admin/rooms" },
    schedule: { label: "Lịch làm việc", icon: CalendarDays, path: "/admin/schedule" },
    adminShiftRequests: { label: "Yêu cầu đổi ca", icon: ClipboardList, path: "/admin/shift-requests" },
    reports: { label: "Báo cáo thống kê", icon: FileText, path: "/admin/reports" },
    
    receptionMySchedule: { label: "Lịch trực cá nhân", icon: CalendarDays, path: "/staff/my-schedule" },
    doctorMySchedule: { label: "Lịch khám cá nhân", icon: CalendarDays, path: "/veterinarian/my-schedule" },
    staffProfile: { label: "Thông tin cá nhân", icon: User, path: "/staff/profile" },
    vetProfile: { label: "Thông tin cá nhân", icon: User, path: "/veterinarian/profile" },
    adminProfile: { label: "Thông tin cá nhân", icon: User, path: "/admin/profile" },
};
