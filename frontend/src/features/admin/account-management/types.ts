export type AccountStatus = "active" | "locked" | "disabled" | "unverified";

export type AccountRole =
    | "admin"
    | "staff"
    | "doctor"
    | "owner";

export interface Account {
    accountId: string;
    fullName: string;
    email: string;
    phone: string;
    roles: AccountRole[];
    status: AccountStatus;
    createdAt: string;
    updatedAt?: string;
    roleCode?: string;
    roleName?: string;
}

export interface AccountSearchParams {
    fullName?: string;
    email?: string;
    phone?: string;
    role?: AccountRole | "";
    status?: AccountStatus | "";
}
