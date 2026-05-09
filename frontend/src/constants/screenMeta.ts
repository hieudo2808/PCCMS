import {
    Lock,
    User,
    ShieldCheck,
    Home,
    PawPrint,
    Stethoscope,
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
    BedDouble,
    ClipboardCheck,
    Receipt,
} from "lucide-react";
import type { ScreenKey, ScreenMetaItem } from "~/types/navigation";

export const screenMeta: Record<ScreenKey, ScreenMetaItem> = {
    // Auth
    login: { label: "Đăng nhập", icon: Lock, path: "/login" },
    register: { label: "Đăng ký", icon: User, path: "/register" },
    forgot: { label: "Quên mật khẩu", icon: ShieldCheck, path: "/forgot-password" },

    // Owner
    ownerDashboard: { label: "Dashboard", icon: Home, path: "/owner" },
    pets: { label: "Hồ sơ thú cưng", icon: PawPrint, path: "/owner/pets" },
    unifiedBooking: { label: "Đặt lịch hẹn", icon: Stethoscope, path: "/owner/book" },
    boardingBooking: { label: "Đặt phòng lưu trú", icon: BedDouble, path: "/owner/boarding/book" },
    boardingTracking: { label: "Theo dõi lưu trú", icon: Camera, path: "/owner/boarding/tracking" },
    payments: { label: "Lịch sử thanh toán", icon: CreditCard, path: "/owner/payments" },
    profile: { label: "Thông tin cá nhân", icon: User, path: "/owner/profile" },

    // Reception
    receptionDashboard: { label: "Dashboard nhân viên", icon: Home, path: "/reception" },
    appointmentReception: {
        label: "Tiếp nhận hẹn khám",
        icon: ClipboardList,
        path: "/reception/appointments",
    },
    groomingBoard: {
        label: "Kanban dịch vụ spa",
        icon: Sparkles,
        path: "/reception/grooming-board",
    },
    boardingReception: {
        label: "Tiếp nhận lưu trú",
        icon: ClipboardCheck,
        path: "/reception/boarding-reception",
    },
    boardingLog: { label: "Nhật ký lưu trú", icon: FileText, path: "/reception/boarding-log" },
    cashier: { label: "Thu ngân", icon: Receipt, path: "/reception/cashier" },

    // Doctor
    doctorDashboard: { label: "Dashboard bác sĩ", icon: Home, path: "/doctor" },
    doctorQueue: { label: "Danh sách chờ khám", icon: HeartPulse, path: "/doctor/queue" },
    medicalRecord: { label: "Bệnh án & kê đơn", icon: FileText, path: "/doctor/medical-record" },

    // Admin
    adminDashboard: { label: "Dashboard quản trị", icon: Home, path: "/admin" },
    accounts: { label: "Quản lý tài khoản", icon: Users, path: "/admin/accounts" },
    catalog: { label: "Danh mục dịch vụ & thuốc", icon: Pill, path: "/admin/catalog" },
    rooms: { label: "Quản lý phòng lưu trú", icon: Warehouse, path: "/admin/rooms" },
    schedule: { label: "Lịch làm việc", icon: CalendarDays, path: "/admin/schedule" },
    reports: { label: "Báo cáo thống kê", icon: FileText, path: "/admin/reports" },
};
