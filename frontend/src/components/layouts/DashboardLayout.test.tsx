import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { DashboardLayout } from "./DashboardLayout";

const logout = vi.fn();

vi.mock("~/features/auth/context/AuthContext", () => ({
    useAuth: () => ({
        user: { fullName: "Nguyen Van A" },
        logout,
    }),
}));

vi.mock("~/shared/notifications", () => ({
    NotificationBell: () => <button type="button" aria-label="Thông báo" />,
}));

function renderLayout() {
    return render(
        <MemoryRouter initialEntries={["/owner/dashboard"]}>
            <Routes>
                <Route path="/owner" element={<DashboardLayout />}>
                    <Route path="dashboard" element={<div>Dashboard content</div>} />
                </Route>
            </Routes>
        </MemoryRouter>,
    );
}

describe("DashboardLayout mobile navigation", () => {
    beforeEach(() => {
        logout.mockClear();
        document.body.style.overflow = "";
    });

    it("opens and closes the sidebar from the mobile menu button", async () => {
        const user = userEvent.setup();
        renderLayout();

        const toggle = screen.getByRole("button", { name: "Mở menu điều hướng" });
        const sidebar = document.getElementById("dashboard-sidebar");

        expect(toggle).toHaveAttribute("aria-expanded", "false");
        expect(sidebar).toHaveClass("invisible", "-translate-x-full");

        await user.click(toggle);

        expect(toggle).toHaveAttribute("aria-expanded", "true");
        expect(toggle).toHaveAccessibleName("Đóng menu điều hướng");
        expect(sidebar).toHaveClass("visible", "translate-x-0");

        await user.click(toggle);

        expect(toggle).toHaveAttribute("aria-expanded", "false");
        expect(sidebar).toHaveClass("invisible", "-translate-x-full");
    });

    it("locks background scrolling and closes the mobile sidebar with Escape", async () => {
        const user = userEvent.setup();
        renderLayout();

        await user.click(screen.getByRole("button", { name: "Mở menu điều hướng" }));

        expect(document.body).toHaveStyle({ overflow: "hidden" });
        expect(screen.getAllByRole("button", { name: "Đóng menu điều hướng" })).toHaveLength(3);

        await user.keyboard("{Escape}");

        expect(screen.getByRole("button", { name: "Mở menu điều hướng" })).toHaveAttribute(
            "aria-expanded",
            "false",
        );
        expect(document.body.style.overflow).toBe("");
    });
});
