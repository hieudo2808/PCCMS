import { describe, it, expect } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { useForm, FormProvider } from "react-hook-form";
import {
    PrescriptionTable,
    type PrescriptionFormValues,
} from "~/features/doctor/components/PrescriptionTable";

function TestWrapper({ disabled = false }: { disabled?: boolean }) {
    const methods = useForm<PrescriptionFormValues>({
        defaultValues: { items: [] },
    });

    return (
        <FormProvider {...methods}>
            <PrescriptionTable disabled={disabled} />
        </FormProvider>
    );
}

describe("PrescriptionTable", () => {
    it("adds and removes rows", async () => {
        render(<TestWrapper />);

        const addButton = screen.getByRole("button", { name: /Thêm thuốc/i });

        // Add row
        await userEvent.click(addButton);
        expect(screen.getAllByPlaceholderText("ID Thuốc (VD: Amoxicillin)")).toHaveLength(1);

        // Remove row
        const deleteButton = screen.getByRole("button", { name: "" }); // Trash2 button has empty text but is the only other button
        await userEvent.click(deleteButton);
        expect(screen.queryByPlaceholderText("ID Thuốc (VD: Amoxicillin)")).not.toBeInTheDocument();
    });

    it("auto calculates total quantity", async () => {
        render(<TestWrapper />);

        await userEvent.click(screen.getByRole("button", { name: /Thêm thuốc/i }));

        const dosageInputs = screen.getAllByPlaceholderText("Liều");
        const freqInputs = screen.getAllByPlaceholderText("Lần");
        const dayInputs = screen.getAllByPlaceholderText("Ngày");

        // Assuming quantity input is the 4th number input (dosage, freq, day, quantity)
        // Wait, let's just type into the inputs and check if quantity input updates
        await userEvent.clear(dosageInputs[0]);
        await userEvent.type(dosageInputs[0], "2");
        await userEvent.clear(freqInputs[0]);
        await userEvent.type(freqInputs[0], "3");
        await userEvent.clear(dayInputs[0]);
        await userEvent.type(dayInputs[0], "5");

        // Expected quantity: 2 * 3 * 5 = 30
        await waitFor(() => {
            const quantityInput = screen.getByDisplayValue("30");
            expect(quantityInput).toBeInTheDocument();
        });
    });

    it("disables inputs when disabled=true", async () => {
        render(<TestWrapper disabled={true} />);

        expect(screen.queryByRole("button", { name: /Thêm thuốc/i })).not.toBeInTheDocument();
    });
});
