import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { RoomsPage } from "~/features/admin/pages/RoomsPage";
import { roomAdminApi } from "~/features/boarding/api/boardingApi";

vi.mock("~/features/boarding/api/boardingApi", () => ({
  roomAdminApi: {
    getRoomTypes: vi.fn(),
    getRooms: vi.fn(),
    createRoomType: vi.fn(),
    createRoom: vi.fn(),
    updateRoomStatus: vi.fn(),
  },
}));

vi.mock("react-hot-toast", () => ({
  default: {
    success: vi.fn(),
    error: vi.fn(),
  },
}));

function renderWithQueryClient(ui: React.ReactElement) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  return render(<QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>);
}

describe("RoomsPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(roomAdminApi.getRoomTypes).mockResolvedValue([
      { id: "type-1", code: "STANDARD", name: "Standard", defaultCapacity: 1, baseDailyPriceVnd: 150000, isActive: true },
    ]);
    vi.mocked(roomAdminApi.getRooms).mockResolvedValue({
      content: [{ id: "room-1", roomCode: "P101", name: "Phong 101", roomTypeId: "type-1", roomTypeName: "Standard", floor: 1, capacity: 1, statusCode: "AVAILABLE" }],
      pageNumber: 1,
      pageSize: 20,
      totalElements: 1,
      totalPages: 1,
      isLast: true,
    });
  });

  it("should_render_room_inventory_when_data_loaded", async () => {
    renderWithQueryClient(<RoomsPage />);

    expect((await screen.findAllByText("Standard")).length).toBeGreaterThan(0);
    expect(await screen.findByText(/P101/)).toBeInTheDocument();
    expect(screen.getAllByText("AVAILABLE").length).toBeGreaterThan(0);
  });
});
