import { render, screen } from "@testing-library/react";
import { describe, it, expect } from "vitest";

describe("App Test", () => {
    it("renders testing library correctly", () => {
        render(<div>Hello, TDD!</div>);
        expect(screen.getByText("Hello, TDD!")).toBeInTheDocument();
    });
});
