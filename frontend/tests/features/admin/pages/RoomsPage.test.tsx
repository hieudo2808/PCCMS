import React from "react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { RoomsPage } from "~/features/admin/pages/RoomsPage";
import { adminRoomApi } from "~/features/admin/api/adminRoomApi";

vi.mock("~/features/admin/api/adminRoomApi", () => ({
    adminRoomApi: {
        listRoomTypes: vi.fn(),
        listRooms: vi.fn(),
        createRoomType: vi.fn(),
        createRoom: vi.fn(),
        updateRoom: vi.fn(),
        deleteRoom: vi.fn(),
        updateRoomType: vi.fn(),
        deleteRoomType: vi.fn(),
    },
}));

vi.mock("react-hot-toast", () => ({
    toast: {
        success: vi.fn(),
        error: vi.fn(),
    },
}));

function renderWithQueryClient(ui: React.ReactElement) {
    const queryClient = new QueryClient({
        defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
    });
    return render(<QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>);
}

describe("RoomsPage", () => {
    beforeEach(() => {
        vi.clearAllMocks();
        vi.mocked(adminRoomApi.listRoomTypes).mockResolvedValue([
            {
                id: "type-1",
                code: "STANDARD",
                name: "Standard",
                defaultCapacity: 1,
                baseDailyPriceVnd: 150000,
                isActive: true,
            },
        ]);

        vi.mocked(adminRoomApi.listRooms).mockResolvedValue({
            content: [
                {
                    id: "room-1",
                    roomCode: "P101",
                    name: "Phòng 101",
                    roomTypeId: "type-1",
                    roomTypeName: "Standard",
                    floor: 1,
                    capacity: 1,
                    statusCode: "AVAILABLE",
                    statusLabel: "Trống",
                },
            ],
            pageNumber: 1,
            pageSize: 20,
            totalElements: 1,
            totalPages: 1,
            isLast: true,
        });
    });

    it("should render rooms tab initially", async () => {
        renderWithQueryClient(<RoomsPage />);
        await waitFor(() => {
            expect(screen.getByText(/Phòng 101/)).toBeInTheDocument();
        });
        expect(screen.getAllByText("Trống").length).toBeGreaterThan(0);
    });

    it("should open create room modal", async () => {
        renderWithQueryClient(<RoomsPage />);
        await waitFor(() => {
            expect(screen.getByText(/Phòng 101/)).toBeInTheDocument();
        });

        await userEvent.click(screen.getByRole("button", { name: /Thêm phòng/i }));

        expect(screen.getByRole("dialog")).toBeInTheDocument();
        expect(screen.getAllByText("Thêm phòng").length).toBeGreaterThan(1);
    });

    it("should switch to room types tab and show create modal", async () => {
        renderWithQueryClient(<RoomsPage />);
        await waitFor(() => {
            expect(screen.getByText(/Phòng 101/)).toBeInTheDocument();
        });

        await userEvent.click(screen.getByRole("button", { name: /Loại phòng/i }));

        await waitFor(() => {
            expect(screen.getAllByText(/Standard/i).length).toBeGreaterThan(0);
        });

        await userEvent.click(screen.getByRole("button", { name: /Thêm loại phòng/i }));

        expect(screen.getByRole("dialog")).toBeInTheDocument();
        expect(screen.getAllByText("Thêm loại phòng").length).toBeGreaterThan(1);
    });
});
