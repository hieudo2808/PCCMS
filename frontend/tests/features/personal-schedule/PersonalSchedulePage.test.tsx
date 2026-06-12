import { render, screen, waitFor, fireEvent } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { PersonalSchedulePage } from "~/features/personal-schedule/PersonalSchedulePage";
import {
    createMyShiftChangeRequest,
    getMyShiftChangeRequests,
    getIncomingShiftChangeRequests,
    respondToIncomingShiftChangeRequest,
    getShiftTargetStaffOptions,
    getMyWorkSchedules,
} from "~/features/personal-schedule/personalScheduleService";

vi.mock("~/features/personal-schedule/personalScheduleService", () => ({
    getMyWorkSchedules: vi.fn(),
    getMyShiftChangeRequests: vi.fn(),
    getIncomingShiftChangeRequests: vi.fn(),
    getShiftTargetStaffOptions: vi.fn(),
    createMyShiftChangeRequest: vi.fn(),
    cancelMyShiftChangeRequest: vi.fn(),
    respondToIncomingShiftChangeRequest: vi.fn(),
}));

describe("PersonalSchedulePage", () => {
    beforeEach(() => {
        vi.clearAllMocks();
        vi.mocked(getMyWorkSchedules).mockResolvedValue([]);
        vi.mocked(getMyShiftChangeRequests).mockResolvedValue([]);
        vi.mocked(getIncomingShiftChangeRequests).mockResolvedValue([]);
        vi.mocked(getShiftTargetStaffOptions).mockResolvedValue([]);
    });

    it("should load and display data on mount", async () => {
        vi.mocked(getMyWorkSchedules).mockResolvedValue([
            {
                id: "ws-1",
                userId: "user-1",
                userName: "Tôi",
                role: "Bác sĩ thú y",
                workDate: new Date(Date.now() + 86400000).toISOString().slice(0, 10), // Tomorrow
                shift: "Ca sáng",
                startTime: "08:00:00",
                endTime: "12:00:00",
                status: "Đã phân công",
                note: "",
            },
        ]);
        vi.mocked(getIncomingShiftChangeRequests).mockResolvedValue([
            {
                requestId: "req-1",
                scheduleId: "ws-2",
                senderName: "Nguyen Van A",
                receiverName: "Tôi",
                reason: "Bận việc gia đình",
                status: "Đang chờ",
                createdAt: new Date().toISOString(),
            },
        ]);

        render(<PersonalSchedulePage />);

        await waitFor(() => {
            expect(getMyWorkSchedules).toHaveBeenCalled();
            expect(getIncomingShiftChangeRequests).toHaveBeenCalled();
        });

        // The UI should show "Ca sáng"
        expect(await screen.findByText("Ca sáng")).toBeInTheDocument();
        
        // The Incoming requests should show the sender name
        expect(screen.getByText("Nguyen Van A")).toBeInTheDocument();
        expect(screen.getByText("Bận việc gia đình")).toBeInTheDocument();
    });

    it("should allow responding to incoming shift change request", async () => {
        vi.mocked(getIncomingShiftChangeRequests).mockResolvedValue([
            {
                requestId: "req-1",
                scheduleId: "ws-2",
                senderName: "Nguyen Van A",
                receiverName: "Tôi",
                reason: "Bận việc gia đình",
                status: "Đang chờ",
                createdAt: new Date().toISOString(),
            },
        ]);
        
        vi.mocked(respondToIncomingShiftChangeRequest).mockResolvedValue({
            requestId: "req-1",
            scheduleId: "ws-2",
            senderName: "Nguyen Van A",
            receiverName: "Tôi",
            reason: "Bận việc gia đình",
            status: "Đã đồng ý",
            createdAt: new Date().toISOString(),
        });

        render(<PersonalSchedulePage />);

        // Click Phản hồi
        const respondButton = await screen.findByRole("button", { name: "Phản hồi" });
        await userEvent.click(respondButton);

        // Expect Dialog to open
        await waitFor(() => {
            expect(screen.getByText("Phản hồi yêu cầu đổi ca")).toBeInTheDocument();
        });

        // Click Đồng ý
        const acceptButton = screen.getByRole("button", { name: "Đồng ý" });
        await userEvent.click(acceptButton);

        await waitFor(() => {
            expect(respondToIncomingShiftChangeRequest).toHaveBeenCalledWith("req-1", true, "");
            expect(screen.getByText("Đã đồng ý đổi ca")).toBeInTheDocument();
        });
    });

    it("should open request change dialog and use autocomplete to select target staff", async () => {
        vi.mocked(getMyWorkSchedules).mockResolvedValue([
            {
                id: "ws-1",
                userId: "user-1",
                userName: "Tôi",
                role: "Bác sĩ thú y",
                workDate: new Date(Date.now() + 86400000 * 2).toISOString().slice(0, 10), // 2 days from now
                shift: "Ca sáng",
                startTime: "08:00:00",
                endTime: "12:00:00",
                status: "Đã phân công",
                note: "",
            },
        ]);
        vi.mocked(getShiftTargetStaffOptions).mockResolvedValue([
            { id: "user-2", fullName: "Tran Van B", roleCode: "VETERINARIAN", roleName: "Bác sĩ thú y" },
        ]);
        
        vi.mocked(createMyShiftChangeRequest).mockResolvedValue({
            requestId: "req-new",
            scheduleId: "ws-1",
            senderName: "Tôi",
            receiverName: "Tran Van B",
            reason: "Tôi có việc bận",
            status: "Đang chờ",
            createdAt: new Date().toISOString(),
        });

        render(<PersonalSchedulePage />);

        const requestButton = await screen.findByRole("button", { name: "Yêu cầu đổi ca" });
        await userEvent.click(requestButton);

        await waitFor(() => {
            expect(screen.getByText("Nhập lý do để quản trị viên xử lý yêu cầu đổi ca")).toBeInTheDocument();
        });

        // Since AutocompleteInput provides an input field, find it by placeholder
        const staffInput = screen.getByPlaceholderText("Chọn nhân viên hoặc để trống");
        
        // Use fireEvent to set the exact value immediately, simulating pasting or selecting from datalist
        fireEvent.change(staffInput, { target: { value: "Tran Van B - Bác sĩ thú y" } });

        const reasonTextarea = screen.getByPlaceholderText("Nhập lý do đổi ca");
        await userEvent.type(reasonTextarea, "Tôi có việc bận");

        const submitButton = screen.getByRole("button", { name: "Gửi yêu cầu" });
        await userEvent.click(submitButton);

        await waitFor(() => {
            expect(createMyShiftChangeRequest).toHaveBeenCalledWith("ws-1", "Tôi có việc bận", "user-2");
            expect(screen.getByText("Gửi yêu cầu đổi ca thành công")).toBeInTheDocument();
        });
    });
});
