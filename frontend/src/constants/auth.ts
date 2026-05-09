import type { RoleKey } from "~/types/navigation";

const VALID_ROLES: RoleKey[] = ["public", "owner", "reception", "doctor", "admin"];
const DEV_ROLE_KEY = "__pccms_dev_role__";

function getStoredRole(): RoleKey {
    const stored = sessionStorage.getItem(DEV_ROLE_KEY) as RoleKey | null;
    return stored && VALID_ROLES.includes(stored) ? stored : "owner";
}

/**
 * Mock authentication state for development/demo.
 * Trong dev: dùng DevRoleSwitcher (nút góc dưới phải) để đổi role mà không cần sửa file.
 */
export const mockAuth: { currentRole: RoleKey; isAuthenticated: boolean } = {
    currentRole: getStoredRole(),
    isAuthenticated: true,
};

export function setDevRole(role: RoleKey) {
    mockAuth.currentRole = role;
    sessionStorage.setItem(DEV_ROLE_KEY, role);
}
