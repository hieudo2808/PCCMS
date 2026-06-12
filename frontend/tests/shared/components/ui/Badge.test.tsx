import { render, screen } from "@testing-library/react";
import { describe, it, expect } from "vitest";
import { Badge } from "~/shared/components/ui/Badge";

describe("Badge", () => {
    it("renders correctly with default variant", () => {
        render(<Badge>Active</Badge>);
        expect(screen.getByText("Active")).toBeInTheDocument();
    });

    it("renders different variants", () => {
        const { rerender } = render(<Badge variant="success">Success</Badge>);
        expect(screen.getByText("Success")).toHaveClass("bg-green-100");

        rerender(<Badge variant="error">Error</Badge>);
        expect(screen.getByText("Error")).toHaveClass("bg-red-100");
    });
});
