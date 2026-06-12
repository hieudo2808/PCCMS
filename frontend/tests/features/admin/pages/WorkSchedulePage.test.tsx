import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { WorkSchedulePage } from "~/features/admin/pages/WorkSchedulePage";
import {
    applyWeeklySchedulePlan,
    getWorkScheduleOptions,
    getWorkSchedules,
    previewWeeklySchedulePlan,
} from "~/features/admin/work-schedule-management/workScheduleService";

vi.mock("~/features/admin/work-schedule-management/workScheduleService", () => ({
    getWorkSchedules: vi.fn(),
    getWorkScheduleOptions: vi.fn(),
    searchWorkSchedules: vi.fn(),
    createWorkSchedule: vi.fn(),
    updateWorkSchedule: vi.fn(),
    cancelWorkSchedule: vi.fn(),
    previewWeeklySchedulePlan: vi.fn(),
    applyWeeklySchedulePlan: vi.fn(),
    roleFromBackend: vi.fn((code) => code === "VETERINARIAN" ? "Bác sĩ thú y" : "Lễ tân"),
}));

const createQueryClient = () =>
    new QueryClient({
        defaultOptions: {
            queries: {
                retry: false,
            },
        },
    });

describe("WorkSchedulePage", () => {
    beforeEach(() => {
        vi.clearAllMocks();
        vi.mocked(previewWeeklySchedulePlan).mockResolvedValue({ createdCount: 0, skippedCount: 0, items: [] });
        vi.mocked(applyWeeklySchedulePlan).mockResolvedValue({ createdCount: 0, skippedCount: 0, items: [] });
        vi.mocked(getWorkScheduleOptions).mockResolvedValue({
            staff: [{ id: "staff-1", fullName: "Nguyen Van A", roleCode: "VETERINARIAN", roleName: "Bác sĩ thú y" }],
            shifts: [{ id: "shift-1", shiftCode: "S", shiftName: "Ca sáng", startTime: "08:00:00", endTime: "12:00:00" }],
            roles: [{ id: "role-1", code: "VETERINARIAN", name: "Bác sĩ thú y" }],
            examRooms: [{ id: "room-1", roomCode: "P01", name: "Phòng khám 1" }],
            groomingStations: [],
        });
    });

    it("should render schedule list correctly", async () => {
        vi.mocked(getWorkSchedules).mockResolvedValue([
            {
                id: "ws-1",
                scheduleCode: "WS001",
                staffId: "staff-1",
                staffName: "Nguyen Van A",
                shiftId: "shift-1",
                roleId: "role-1",
                examRoomId: "room-1",
                stationId: "",
                capacity: 1,
                statusCode: "ASSIGNED",
                role: "Bác sĩ thú y",
                room: "Khu khám bệnh",
                position: "P01 - Phòng khám 1",
                workDate: new Date().toISOString().slice(0, 10),
                shift: "Ca sáng",
                status: "Đã phân công",
                note: "",
                source: "backend",
            },
        ]);

        render(
            <QueryClientProvider client={createQueryClient()}>
                <WorkSchedulePage />
            </QueryClientProvider>
        );

        await waitFor(() => {
            expect(screen.getByText("Quản lý lịch làm việc nhân sự")).toBeInTheDocument();
        });

        expect(await screen.findByText("Nguyen Van A")).toBeInTheDocument();
        expect(screen.getByText(/Khu khám bệnh/)).toBeInTheDocument();
    });

    it("should keep manual creation as an override action", async () => {
        vi.mocked(getWorkSchedules).mockResolvedValue([]);

        render(
            <QueryClientProvider client={createQueryClient()}>
                <WorkSchedulePage />
            </QueryClientProvider>
        );

        await waitFor(() => {
            expect(screen.getAllByRole("button", { name: "Thêm thủ công" }).length).toBeGreaterThan(0);
        });

        await userEvent.click(screen.getAllByRole("button", { name: "Thêm thủ công" })[0]);

        await waitFor(() => {
            expect(screen.getByText("Chọn dữ liệu nhân sự, ca và vai trò đang có trên hệ thống")).toBeInTheDocument();
        });
    });

    it("should render generated weekly preview items without a source schedule", async () => {
        vi.mocked(getWorkSchedules).mockResolvedValue([]);
        vi.mocked(previewWeeklySchedulePlan).mockResolvedValue({
            createdCount: 0,
            skippedCount: 0,
            items: [
                {
                    sourceScheduleId: null,
                    createdScheduleId: null,
                    staffId: "staff-1",
                    staffName: "Nguyen Van A",
                    sourceDate: null,
                    targetDate: "2026-06-08",
                    shiftId: "shift-1",
                    shiftCode: "S",
                    shiftName: "Ca sáng",
                    roleId: "role-1",
                    roleCode: "VETERINARIAN",
                    conflict: false,
                    conflictReason: null,
                },
            ],
        });

        render(
            <QueryClientProvider client={createQueryClient()}>
                <WorkSchedulePage />
            </QueryClientProvider>
        );

        await userEvent.click(await screen.findByRole("button", { name: "Preview lịch tuần" }));

        await waitFor(() => {
            expect(previewWeeklySchedulePlan).toHaveBeenCalled();
            expect(screen.getByText("Nguyen Van A")).toBeInTheDocument();
            expect(screen.getByText("2026-06-08")).toBeInTheDocument();
            expect(screen.getByText("Có thể tạo")).toBeInTheDocument();
        });
    });
});
