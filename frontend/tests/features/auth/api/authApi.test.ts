import { describe, it, expect, vi, beforeEach } from "vitest";
import { server } from "@tests/mocks/server";
import { http, HttpResponse } from "msw";
import { authApi } from "~/features/auth/api/authApi";
import type { LoginRequest, RegisterRequest } from "~/types";

describe("authApi", () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it("login successfully", async () => {
        server.use(
            http.post("*/auth/login", () => {
                return HttpResponse.json({
                    success: true,
                    code: 200,
                    message: "Success",
                    data: {
                        token: "test-token",
                        refreshToken: "test-refresh",
                        user: { id: "1", roleCode: "OWNER" },
                    },
                });
            })
        );

        const payload: LoginRequest = { email: "test@test.com", password: "password" };
        const res = await authApi.login(payload);
        expect(res.token).toBe("test-token");
        expect(res.user.roleCode).toBe("OWNER");
    });

    it("register successfully", async () => {
        server.use(
            http.post("*/auth/register", () => {
                return HttpResponse.json({
                    success: true,
                    code: 201,
                    message: "Created",
                    data: {
                        token: "test-token-reg",
                        refreshToken: "test-refresh-reg",
                        user: { id: "2", roleCode: "OWNER" },
                    },
                });
            })
        );

        const payload: RegisterRequest = {
            email: "new@test.com",
            password: "password",
            fullName: "Test",
        };
        const res = await authApi.register(payload);
        expect(res.token).toBe("test-token-reg");
    });

    it("refresh token successfully", async () => {
        server.use(
            http.post("*/auth/refresh", () => {
                return HttpResponse.json({
                    success: true,
                    code: 200,
                    message: "Success",
                    data: {
                        token: "new-token",
                        refreshToken: "new-refresh",
                        user: { id: "1", roleCode: "OWNER" },
                    },
                });
            })
        );

        const res = await authApi.refreshToken();
        expect(res.token).toBe("new-token");
    });
});
