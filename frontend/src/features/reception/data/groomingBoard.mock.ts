// ─── Mock Data – Grooming Board ──────────────────────────────────────────────
// TODO: Replace with API call → GET /api/grooming/tickets/today
// when backend is ready.

export type GroomingStatus = "Chờ làm" | "Đang dùng dịch vụ" | "Hoàn thành";

export interface ServiceTicket {
    id: string;
    petName: string;
    ownerName: string;
    service: string;
    slot: string;
    note?: string;
    status: GroomingStatus;
    updatedAt: string;
}

export const GROOMING_COLUMNS: GroomingStatus[] = [
    "Chờ làm",
    "Đang dùng dịch vụ",
    "Hoàn thành",
];

export const GROOMING_TRANSITIONS: Record<GroomingStatus, GroomingStatus | null> = {
    "Chờ làm": "Đang dùng dịch vụ",
    "Đang dùng dịch vụ": "Hoàn thành",
    "Hoàn thành": null,
};

export const GROOMING_TRANSITION_LABELS: Record<GroomingStatus, string> = {
    "Chờ làm": "Bắt đầu làm",
    "Đang dùng dịch vụ": "Hoàn thành",
    "Hoàn thành": "",
};

export const MOCK_GROOMING_TICKETS: ServiceTicket[] = [
    {
        id: "SPA101",
        petName: "Milu",
        ownerName: "Nguyễn Văn A",
        service: "Tắm + Sấy + Cắt tỉa",
        slot: "09:00 - 10:00",
        note: "Bé sợ máy sấy",
        status: "Chờ làm",
        updatedAt: "08:45",
    },
    {
        id: "SPA102",
        petName: "Luna",
        ownerName: "Trần Thị B",
        service: "Cắt móng",
        slot: "09:00 - 10:00",
        status: "Chờ làm",
        updatedAt: "08:52",
    },
    {
        id: "SPA103",
        petName: "Mít",
        ownerName: "Phạm Văn C",
        service: "Spa Premium",
        slot: "09:00 - 10:30",
        note: "Dị ứng với mùi lavender",
        status: "Đang dùng dịch vụ",
        updatedAt: "09:10",
    },
    {
        id: "SPA104",
        petName: "Bơ",
        ownerName: "Lê Thị D",
        service: "Tắm + Sấy cơ bản",
        slot: "08:00 - 09:00",
        status: "Hoàn thành",
        updatedAt: "09:05",
    },
    {
        id: "SPA105",
        petName: "Táo",
        ownerName: "Hoàng Văn E",
        service: "Vệ sinh tai + răng",
        slot: "10:00 - 10:30",
        status: "Chờ làm",
        updatedAt: "09:30",
    },
];
