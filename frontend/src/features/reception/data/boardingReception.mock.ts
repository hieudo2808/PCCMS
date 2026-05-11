// ─── Mock Data – Boarding Reception ──────────────────────────────────────────
// TODO: Replace with API call → GET /api/boarding/requests
// when backend is ready.

// FIX #4: Thêm trạng thái "Đã trả" để hoàn chỉnh vòng đời lưu trú
export type BookingStatus =
    | "Chờ tiếp nhận"
    | "Đã tiếp nhận"
    | "Đang lưu trú"
    | "Đã trả"
    | "Đã từ chối";

export interface BoardingRequest {
    id: string;
    code: string;
    ownerName: string;
    ownerPhone: string;
    petName: string;
    breed: string;
    roomType: string;
    checkIn: string;
    checkOut: string;
    nights: number;
    totalPrice: number;
    note: string;
    status: BookingStatus;
    createdAt: string;
}

export const STATUS_TONE: Record<BookingStatus, "amber" | "blue" | "green" | "red"> = {
    "Chờ tiếp nhận": "amber",
    "Đã tiếp nhận": "blue",
    "Đang lưu trú": "green",
    "Đã trả": "green",
    "Đã từ chối": "red",
};

export const MOCK_BOARDING_REQUESTS: BoardingRequest[] = [
    {
        id: "1",
        code: "BRD-001",
        ownerName: "Nguyễn Văn A",
        ownerPhone: "0912 345 678",
        petName: "Milu",
        breed: "Poodle",
        roomType: "Chuồng Deluxe",
        checkIn: "01/06/2026",
        checkOut: "04/06/2026",
        nights: 3,
        totalPrice: 840_000,
        note: "Bé sợ tiếng ồn, ăn hạt lúc 7h sáng và 6h tối.",
        status: "Chờ tiếp nhận",
        createdAt: "02/05/2026 - 03:15",
    },
    {
        id: "2",
        code: "BRD-002",
        ownerName: "Trần Thị B",
        ownerPhone: "0987 654 321",
        petName: "Luna",
        breed: "Golden Retriever",
        roomType: "Chuồng tiêu chuẩn",
        checkIn: "05/06/2026",
        checkOut: "08/06/2026",
        nights: 3,
        totalPrice: 450_000,
        note: "",
        status: "Chờ tiếp nhận",
        createdAt: "02/05/2026 - 02:50",
    },
    {
        id: "3",
        code: "BRD-003",
        ownerName: "Phạm Văn C",
        ownerPhone: "0901 111 222",
        petName: "Mít",
        breed: "Mèo ALN",
        roomType: "Phòng VIP",
        checkIn: "10/06/2026",
        checkOut: "15/06/2026",
        nights: 5,
        totalPrice: 2_500_000,
        note: "Bé hay trốn, cần đóng cửa kỹ.",
        status: "Đang lưu trú",
        createdAt: "01/05/2026 - 18:00",
    },
];
