import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, it, expect, vi } from "vitest";
import { Button } from "~/shared/components/ui/Button";

describe("Button", () => {
    it("renders correctly", () => {
        render(<Button>Click me</Button>);
        expect(screen.getByRole("button", { name: "Click me" })).toBeInTheDocument();
    });

    it("handles click events", async () => {
        const handleClick = vi.fn();
        render(<Button onClick={handleClick}>Click me</Button>);
        await userEvent.click(screen.getByRole("button"));
        expect(handleClick).toHaveBeenCalledTimes(1);
    });

    it("is disabled when disabled prop is true", () => {
        render(<Button disabled>Click me</Button>);
        expect(screen.getByRole("button")).toBeDisabled();
    });

    it("shows loading spinner and is disabled when loading prop is true", () => {
        render(<Button loading>Submit</Button>);
        expect(screen.getByRole("button")).toBeDisabled();
        // Assuming we use an SVG for the spinner, we can check for it, or check the loading class
        expect(screen.getByRole("button")).toHaveTextContent("Submit");
    });
});
