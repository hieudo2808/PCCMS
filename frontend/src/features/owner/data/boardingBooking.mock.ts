// ─── Mock Data – Boarding Booking Page (Owner) ───────────────────────────────
// TODO: Replace with API call:
//   - GET /api/pets/mine        → danh sách thú cưng của chủ nhân đang đăng nhập
//   - GET /api/boarding/rooms   → danh sách loại chuồng và số phòng trống
// when backend is ready.

export const MOCK_OWNER_PETS: string[] = [
    "Milu (Poodle)",
    "Mít (Mèo ALN)",
    "Bơ (Corgi)",
];

export interface RoomType {
    id: string;
    name: string;
    desc: string;
    pricePerDay: number;
    available: number;
    tone: "blue" | "amber" | "green";
}

export const MOCK_ROOM_TYPES: RoomType[] = [
    {
        id: "standard",
        name: "Chuồng tiêu chuẩn",
        desc: "Phòng riêng, có giường nằm, quạt, vệ sinh 2 lần/ngày",
        pricePerDay: 150_000,
        available: 3,
        tone: "blue",
    },
    {
        id: "deluxe",
        name: "Chuồng Deluxe",
        desc: "Phòng rộng hơn, điều hoà, đệm êm, vệ sinh 3 lần/ngày",
        pricePerDay: 280_000,
        available: 1,
        tone: "amber",
    },
    {
        id: "vip",
        name: "Phòng VIP",
        desc: "Phòng đơn cao cấp, TV, camera theo dõi, chăm sóc cá nhân",
        pricePerDay: 500_000,
        available: 0,
        tone: "green",
    },
];
