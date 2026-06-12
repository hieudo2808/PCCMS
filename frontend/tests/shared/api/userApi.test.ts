import { describe, it, expect, vi } from "vitest";
import { userApi } from "~/shared/api/userApi";
import axiosClient from "~/shared/api/axiosClient";

vi.mock("~/shared/api/axiosClient");

describe("userApi", () => {
    const mockUser = {
        userId: "123",
        fullName: "Test User",
        email: "test@example.com",
        avatarUrl: "",
        bio: "",
        roleName: "CUSTOMER",
        createdAt: "2023-01-01",
        statusCode: "ACTIVE",
    };

    it("fetches profile successfully", async () => {
        vi.mocked(axiosClient.get).mockResolvedValueOnce(mockUser);

        const result = await userApi.getProfile();
        expect(result).toEqual(mockUser);
        expect(axiosClient.get).toHaveBeenCalledWith("/v1/users/me");
    });

    it("updates profile successfully", async () => {
        vi.mocked(axiosClient.put).mockResolvedValueOnce(mockUser);

        const result = await userApi.updateProfile({ fullName: "New Name" });
        expect(result).toEqual(mockUser);
        expect(axiosClient.put).toHaveBeenCalledWith("/v1/users/me", { fullName: "New Name" });
    });

    it("changes password successfully", async () => {
        vi.mocked(axiosClient.put).mockResolvedValueOnce(undefined);

        await userApi.changePassword({ currentPassword: "123", newPassword: "456" });
        expect(axiosClient.put).toHaveBeenCalledWith("/v1/users/me/password", {
            currentPassword: "123",
            newPassword: "456",
        });
    });
});
