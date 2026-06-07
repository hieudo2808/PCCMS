import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, it, expect, vi } from "vitest";
import { ErrorState } from "~/shared/components/ErrorState/index";

describe("ErrorState", () => {
    it("renders correctly with message and retry button", async () => {
        const handleRetry = vi.fn();
        render(<ErrorState message="Something went wrong" onRetry={handleRetry} />);
        expect(screen.getByText("Something went wrong")).toBeInTheDocument();

        const retryBtn = screen.getByRole("button", { name: "Thử lại" });
        await userEvent.click(retryBtn);
        expect(handleRetry).toHaveBeenCalledTimes(1);
    });
});
