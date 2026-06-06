import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { groomingApi } from "~/features/grooming/api/groomingApi";
import { GroomingTrackingPage } from "~/features/owner/pages/GroomingTrackingPage";

vi.mock("~/features/grooming/api/groomingApi", () => ({
  groomingApi: {
    getMyTickets: vi.fn(),
    cancelTicket: vi.fn(),
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

describe("GroomingTrackingPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(groomingApi.getMyTickets).mockResolvedValue({
      content: [
        {
          id: "ticket-1",
          appointmentId: "appointment-1",
          serviceOrderId: "order-1",
          orderCode: "SO-001",
          serviceOrderStatus: "REQUESTED",
          ownerId: "owner-1",
          ownerName: "Owner",
          petId: "pet-1",
          petName: "Milu",
          serviceId: "service-1",
          serviceCode: "GRM-BATH",
          serviceName: "Tam say",
          basePriceVnd: 100000,
          durationMinutes: 60,
          scheduledStartAt: "2026-06-10T09:00:00+07:00",
          scheduledEndAt: "2026-06-10T10:00:00+07:00",
          appointmentStatus: "PENDING",
          statusCode: "PENDING",
          ownerNote: "Can nhe tay",
          estimatedAmountVnd: 100000,
        },
      ],
      pageNumber: 1,
      pageSize: 10,
      totalElements: 1,
      totalPages: 1,
      isLast: true,
    });
    vi.mocked(groomingApi.cancelTicket).mockResolvedValue({} as never);
  });

  it("should_allow_owner_cancel_pending_grooming_ticket", async () => {
    renderWithQueryClient(<GroomingTrackingPage />);

    await screen.findByText("Milu - Tam say");
    await userEvent.click(screen.getByRole("button", { name: /Hủy lịch/i }));

    await waitFor(() => {
      expect(groomingApi.cancelTicket).toHaveBeenCalledWith("ticket-1", "Chủ nuôi hủy lịch trước khi nhân viên duyệt");
    });
  });
});
