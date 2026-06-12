import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, it, expect, vi, beforeEach } from "vitest";
import { RouterProvider } from "react-router-dom";
import { router } from "~/router";
import { AuthProvider } from "~/features/auth/context/AuthContext";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";

describe("Router 404 Catch-all", () => {
    const queryClient = new QueryClient();

    beforeEach(() => {
        // Mock navigate directly on the router's history
        vi.spyOn(router, 'navigate');
    });

    it("uses SPA navigation for the 'Về trang chủ' button on 404 page", async () => {
        // Temporarily navigate to a missing page
        await router.navigate("/this-page-does-not-exist");

        render(
            <QueryClientProvider client={queryClient}>
                <AuthProvider>
                    <RouterProvider router={router} />
                </AuthProvider>
            </QueryClientProvider>
        );

        // Find the 404 page button
        expect(screen.getByText("404")).toBeInTheDocument();
        const homeBtn = screen.getByRole("link", { name: "Về trang chủ" });
        
        expect(homeBtn).toHaveAttribute("href", "/");
        
        await userEvent.click(homeBtn);

        // If it's a native <a> tag, userEvent.click might throw "Not implemented: navigation" 
        // Let's assert that router state changes (meaning Link was used and it caught the click)
        expect(router.state.location.pathname).toBe("/login");
    });
});
