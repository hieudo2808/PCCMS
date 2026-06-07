import { describe, it, expect, vi, beforeEach } from "vitest";
import { server } from "@tests/mocks/server";
import { http, HttpResponse } from "msw";
import axiosClient from "~/shared/api/axiosClient";
import toast from "react-hot-toast";

vi.mock("react-hot-toast", () => ({
    default: {
        error: vi.fn(),
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

    it("redirects to /login and clears token on 401", async () => {
        localStorage.setItem("token", "expired-token");

        // mock window.location.href
        const originalLocation = window.location;
        // @ts-ignore
        delete window.location;
        // @ts-ignore
        window.location = { href: "" };

        server.use(
            http.get("*/test-401", () => {
                return new HttpResponse(null, { status: 401 });
            })
        );

        await expect(axiosClient.get("/test-401")).rejects.toThrow();
        expect(localStorage.getItem("token")).toBeNull();
        expect(window.location.href).toBe("/login");

        // @ts-ignore
        window.location = originalLocation;
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
