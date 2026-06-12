import { render, screen } from "@testing-library/react";
import { describe, it, expect } from "vitest";
import { EmptyState } from "~/shared/components/EmptyState/index";
import { FileSearch } from "lucide-react";

describe("EmptyState", () => {
    it("renders correctly with title and description", () => {
        render(
            <EmptyState
                icon={FileSearch}
                title="No data"
                description="There is no data available"
            />
        );
        expect(screen.getByText("No data")).toBeInTheDocument();
        expect(screen.getByText("There is no data available")).toBeInTheDocument();
    });
});
