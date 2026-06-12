import { describe, it, expect, vi, beforeEach } from "vitest";
import { server } from "@tests/mocks/server";
import { http, HttpResponse } from "msw";
import axiosClient from "~/shared/api/axiosClient";
import { router } from "~/router";
import toast from "react-hot-toast";

vi.mock("react-hot-toast", () => ({
    default: {
        error: vi.fn(),
    },
}));

vi.mock("~/router", () => ({
    router: {
        navigate: vi.fn(),
    },
}));

describe("axiosClient", () => {
    beforeEach(() => {
        localStorage.clear();
        vi.clearAllMocks();
        axiosClient.defaults.adapter = "http";
        axiosClient.defaults.baseURL = "http://localhost:8080/api";
    });

    it("unwraps response.data.data automatically", async () => {
        server.use(
            http.get("*/test-unwrap", () => {
                return HttpResponse.json({
                    success: true,
                    code: 200,
                    message: "OK",
                    data: { id: 1, name: "Test" },
                });
            })
        );

        const response = await axiosClient.get("/test-unwrap");
        // Since we unwrap data.data, the return of axiosClient.get should be { id: 1, name: 'Test' }
        // Wait, axios interceptors can return response.data.data, but TypeScript type AxiosResponse usually expects the whole object.
        // If we return response.data.data, we cast it.
        expect(response).toEqual({ id: 1, name: "Test" });
    });

    it("redirects to /login and clears token on 401 (non-auth endpoints)", async () => {
        localStorage.setItem("token", "expired-token");

        // mock window.location.href to ensure it is NOT changed
        const originalLocation = window.location;
        // @ts-ignore
        delete window.location;
        // @ts-ignore
        window.location = { href: "http://localhost/protected" };

        server.use(
            http.get("*/test-401", () => {
                return new HttpResponse(null, { status: 401 });
            })
        );

        await expect(axiosClient.get("/test-401")).rejects.toThrow();
        expect(localStorage.getItem("token")).toBeNull();
        expect(window.location.href).toBe("http://localhost/protected"); // Should NOT change
        expect(router.navigate).toHaveBeenCalledWith("/login");

        // @ts-ignore
        window.location = originalLocation;
    });

    it("does not navigate to /login if 401 is from an auth endpoint", async () => {
        server.use(
            http.post("*/auth/login", () => {
                return new HttpResponse(null, { status: 401 });
            })
        );

        await expect(axiosClient.post("/auth/login")).rejects.toThrow();
        expect(router.navigate).not.toHaveBeenCalled();
    });

    it("shows toast error on 403", async () => {
        server.use(
            http.get("*/test-403", () => {
                return new HttpResponse(null, { status: 403 });
            })
        );

        await expect(axiosClient.get("/test-403")).rejects.toThrow();
        expect(toast.error).toHaveBeenCalledWith("Không có quyền truy cập");
    });
});
