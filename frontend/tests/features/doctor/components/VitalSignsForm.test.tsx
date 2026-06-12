import { describe, it, expect, vi } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { VitalSignsForm } from "~/features/doctor/components/VitalSignsForm";

describe("VitalSignsForm", () => {
    it("validates SpO2 > 100 and shows error", async () => {
        render(<VitalSignsForm />);

        const spo2Input = screen.getByLabelText("SpO2 (%)");

        await userEvent.type(spo2Input, "105");

        // trigger validation by submitting or blurring, React Hook Form might need a submit if mode is not onChange
        const draftButton = screen.getByRole("button", { name: "Lưu nháp" });
        await userEvent.click(draftButton);

        await waitFor(() => {
            expect(screen.getByText("SpO2 <= 100")).toBeInTheDocument();
        });
    });

    it("disables all inputs when disabled=true is passed", () => {
        render(<VitalSignsForm disabled={true} />);

        expect(screen.getByLabelText("Nhiệt độ (°C)")).toBeDisabled();
        expect(screen.getByLabelText("Nhịp tim")).toBeDisabled();
        expect(screen.getByLabelText("SpO2 (%)")).toBeDisabled();
        expect(screen.getByLabelText("Chẩn đoán ban đầu")).toBeDisabled();

        // Buttons should not be rendered
        expect(screen.queryByRole("button", { name: "Lưu nháp" })).not.toBeInTheDocument();
    });

    it("calls onSaveDraft with correct data", async () => {
        const mockSave = vi.fn();
        render(<VitalSignsForm onSaveDraft={mockSave} />);

        await userEvent.type(screen.getByLabelText("Nhiệt độ (°C)"), "38.5");
        await userEvent.type(screen.getByLabelText("SpO2 (%)"), "98");

        const draftButton = screen.getByRole("button", { name: "Lưu nháp" });
        await userEvent.click(draftButton);

        await waitFor(() => {
            expect(mockSave).toHaveBeenCalledWith(
                expect.objectContaining({
                    temperatureC: 38.5,
                    spo2Percent: 98,
                })
            );
        });
    });
});
