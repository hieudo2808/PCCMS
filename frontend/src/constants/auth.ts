import type { RoleKey } from "~/types/navigation";

export const mockAuth = {
    /**
     * Đổi giá trị này để Test các luồng khác nhau.
     * Giá trị hợp lệ: "public" | "owner" | "reception" | "doctor" | "admin"
     */
    currentRole: "owner" as RoleKey,

    isAuthenticated: true,
};
