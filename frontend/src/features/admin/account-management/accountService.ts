import api, { getApiData, getPageContent } from "~/api/api";
import type { Account, AccountRole, AccountSearchParams, AccountStatus } from "./types";

type BackendAccountStatus = "UNVERIFIED" | "ACTIVE" | "LOCKED" | "DISABLED";
type BackendAccountRole = "ADMIN" | "STAFF" | "VETERINARIAN" | "OWNER" | string;

interface BackendAccount {
    id: string;
    email: string;
    phone?: string;
    fullName?: string;
    roleCode?: BackendAccountRole;
    roleName?: string;
    roles?: BackendAccountRole[];
    statusCode: BackendAccountStatus;
    createdAt?: string;
    updatedAt?: string;
}

interface AccountCredentialResponse {
    account: BackendAccount;
    temporaryPassword: string;
    emailSent?: boolean;
}

export interface AccountCredential {
    account: Account;
    temporaryPassword: string;
    emailSent: boolean;
}

export interface AccountPayload {
    fullName: string;
    email: string;
    phone: string;
    roleCode: string;
    statusCode?: BackendAccountStatus;
}

const roleToBackend: Record<AccountRole, string> = {
    admin: "ADMIN",
    staff: "STAFF",
    doctor: "VETERINARIAN",
    owner: "OWNER",
};

const roleFromBackend = (role?: BackendAccountRole): AccountRole => {
    switch (role) {
        case "ADMIN":
            return "admin";
        case "STAFF":
            return "staff";
        case "VETERINARIAN":
            return "doctor";
        default:
            return "owner";
    }
};

const statusToBackend: Record<AccountStatus, BackendAccountStatus> = {
    active: "ACTIVE",
    locked: "LOCKED",
    disabled: "DISABLED",
    unverified: "UNVERIFIED",
};

const statusFromBackend = (status: BackendAccountStatus): AccountStatus => {
    switch (status) {
        case "ACTIVE":
            return "active";
        case "LOCKED":
            return "locked";
        case "DISABLED":
            return "disabled";
        default:
            return "unverified";
    }
};

function toAccount(account: BackendAccount): Account {
    const roles = account.roles?.length
        ? account.roles.map(roleFromBackend)
        : [roleFromBackend(account.roleCode)];
    return {
        accountId: account.id,
        fullName: account.fullName ?? "",
        email: account.email,
        phone: account.phone ?? "",
        roles,
        status: statusFromBackend(account.statusCode),
        createdAt: account.createdAt ?? "",
        updatedAt: account.updatedAt,
        roleCode: account.roleCode,
        roleName: account.roleName,
    };
}

export const getAllAccounts = async () => searchAccounts({});

export const searchAccounts = async (params: AccountSearchParams) => {
    const keyword = [params.fullName, params.email, params.phone]
        .map((value) => value?.trim())
        .filter(Boolean)
        .join(" ");
    const query = {
        keyword: keyword || undefined,
        role: params.role ? roleToBackend[params.role] : undefined,
        status: params.status ? statusToBackend[params.status] : undefined,
        page: 0,
        size: 50,
    };

    const response = await api.get("/v1/admin/accounts", { params: query });
    const payload = getApiData<unknown>(response);
    return getPageContent<BackendAccount>(payload).map(toAccount);
};

export const updateAccountStatus = async (accountId: string, status: AccountStatus) => {
    const response = await api.patch(`/v1/admin/accounts/${accountId}/status`, {
        statusCode: statusToBackend[status],
    });
    return toAccount(getApiData<BackendAccount>(response));
};

export const updateAccountRoles = async (accountId: string, roles: AccountRole[]) => {
    const roleCode = roleToBackend[roles[0] ?? "staff"];
    const response = await api.patch(`/v1/admin/accounts/${accountId}/role`, { roleCode });
    return toAccount(getApiData<BackendAccount>(response));
};

const toCredential = (response: AccountCredentialResponse): AccountCredential => ({
    account: toAccount(response.account),
    temporaryPassword: response.temporaryPassword,
    emailSent: response.emailSent ?? false,
});

export const createAccount = async (payload: AccountPayload) => {
    const response = await api.post("/v1/admin/accounts", payload);
    return toCredential(getApiData<AccountCredentialResponse>(response));
};

export const updateAccount = async (accountId: string, payload: AccountPayload) => {
    const response = await api.put(`/v1/admin/accounts/${accountId}`, payload);
    return toAccount(getApiData<BackendAccount>(response));
};

export const resetAccountPassword = async (accountId: string) => {
    const response = await api.post(`/v1/admin/accounts/${accountId}/password/reset`);
    return toCredential(getApiData<AccountCredentialResponse>(response));
};
