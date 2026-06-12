import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, it, expect, vi, beforeEach } from "vitest";
import { AuthProvider, useAuth } from "~/features/auth/context/AuthContext";

// Mock jwt-decode
vi.mock("jwt-decode", () => ({
    jwtDecode: (token: string) => {
        if (token === "valid-token") {
            return { exp: Date.now() / 1000 + 3600 }; // 1 hour from now
        }
        throw new Error("Invalid token");
    },
}));

function TestComponent() {
    const { isAuthenticated, user, login, logout } = useAuth();
    return (
        <div>
            <div data-testid="auth-status">{isAuthenticated ? "Auth" : "NotAuth"}</div>
            <div data-testid="user-role">{user?.roleCode || "None"}</div>
            <button
                onClick={() =>
                    login("valid-token", { id: "1", roleCode: "OWNER" } as any)
                }
            >
                Login
            </button>
            <button onClick={logout}>Logout</button>
        </div>
    );
}

describe("AuthContext", () => {
    beforeEach(() => {
        localStorage.clear();
        vi.clearAllMocks();
    });

    it("provides default unauthenticated state", () => {
        render(
            <AuthProvider>
                <TestComponent />
            </AuthProvider>
        );
        expect(screen.getByTestId("auth-status")).toHaveTextContent("NotAuth");
    });

    it("logs in and updates state", async () => {
        render(
            <AuthProvider>
                <TestComponent />
            </AuthProvider>
        );
        await userEvent.click(screen.getByText("Login"));
        expect(screen.getByTestId("auth-status")).toHaveTextContent("Auth");
        expect(screen.getByTestId("user-role")).toHaveTextContent("OWNER");
        expect(localStorage.getItem("token")).toBe("valid-token");
    });

    it("logs out and clears state", async () => {
        render(
            <AuthProvider>
                <TestComponent />
            </AuthProvider>
        );
        await userEvent.click(screen.getByText("Login"));
        await userEvent.click(screen.getByText("Logout"));
        expect(screen.getByTestId("auth-status")).toHaveTextContent("NotAuth");
        expect(localStorage.getItem("token")).toBeNull();
    });

    it("initializes from localStorage if valid token exists", () => {
        localStorage.setItem("token", "valid-token");
        localStorage.setItem("user", JSON.stringify({ id: "2", roleCode: "ADMIN" }));

        render(
            <AuthProvider>
                <TestComponent />
            </AuthProvider>
        );
        expect(screen.getByTestId("auth-status")).toHaveTextContent("Auth");
        expect(screen.getByTestId("user-role")).toHaveTextContent("ADMIN");
    });
});
