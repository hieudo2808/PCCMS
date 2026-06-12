import { render } from "@testing-library/react";
import { describe, it, expect } from "vitest";
import { SkeletonLoader } from "~/shared/components/SkeletonLoader";

describe("SkeletonLoader", () => {
    it("renders correct number of rows", () => {
        const { container } = render(<SkeletonLoader rows={3} />);
        // Should render 3 elements with animate-pulse
        const pulseElements = container.querySelectorAll(".animate-pulse");
        expect(pulseElements).toHaveLength(3);
    });
});
