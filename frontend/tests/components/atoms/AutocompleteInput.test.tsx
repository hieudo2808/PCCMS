import { render, screen, fireEvent } from "@testing-library/react";
import { describe, it, expect, vi } from "vitest";
import { AutocompleteInput } from "~/components/atoms/AutocompleteInput";

describe("AutocompleteInput", () => {
    const mockOptions = [
        { id: "1", label: "Apple" },
        { id: "2", label: "Banana" },
        { id: "3", label: "Cherry" },
    ];

    it("renders input correctly", () => {
        render(<AutocompleteInput options={mockOptions} value="" onChange={vi.fn()} />);
        expect(screen.getByRole("combobox")).toBeInTheDocument();
    });

    it("displays the correct label for a given value", () => {
        render(<AutocompleteInput options={mockOptions} value="2" onChange={vi.fn()} />);
        expect(screen.getByRole("combobox")).toHaveValue("Banana");
    });

    it("triggers onChange with id when an option is selected from datalist", () => {
        const handleChange = vi.fn();
        render(<AutocompleteInput options={mockOptions} value="" onChange={handleChange} />);
        
        const input = screen.getByRole("combobox");
        fireEvent.change(input, { target: { value: "Cherry" } });
        
        expect(handleChange).toHaveBeenCalledWith("3");
    });

    it("triggers onChange with empty string if value doesn't match any label", () => {
        const handleChange = vi.fn();
        render(<AutocompleteInput options={mockOptions} value="" onChange={handleChange} />);
        
        const input = screen.getByRole("combobox");
        fireEvent.change(input, { target: { value: "Random Text" } });
        
        expect(handleChange).toHaveBeenCalledWith("");
    });
});
