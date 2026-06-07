import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, it, expect, vi } from "vitest";
import { ConfirmDialog } from "~/shared/components/ConfirmDialog/index";

describe("ConfirmDialog", () => {
    it("renders nothing when isOpen is false", () => {
        const { container } = render(
            <ConfirmDialog
                isOpen={false}
                title="Delete"
                message="Are you sure?"
                onConfirm={vi.fn()}
                onCancel={vi.fn()}
            />
        );
        expect(container).toBeEmptyDOMElement();
    });

    it("renders modal when isOpen is true and handles interactions", async () => {
        const handleConfirm = vi.fn();
        const handleCancel = vi.fn();

        render(
            <ConfirmDialog
                isOpen={true}
                title="Delete Item"
                message="Are you sure you want to delete this?"
                onConfirm={handleConfirm}
                onCancel={handleCancel}
            />
        );

        expect(screen.getByText("Delete Item")).toBeInTheDocument();
        expect(screen.getByText("Are you sure you want to delete this?")).toBeInTheDocument();

        const cancelBtn = screen.getByRole("button", { name: "Hủy" });
        const confirmBtn = screen.getByRole("button", { name: "Xác nhận" });

        await userEvent.click(cancelBtn);
        expect(handleCancel).toHaveBeenCalledTimes(1);

        await userEvent.click(confirmBtn);
        expect(handleConfirm).toHaveBeenCalledTimes(1);
    });
});
