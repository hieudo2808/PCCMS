// ─── Mock Data – Boarding Log ─────────────────────────────────────────────────
// TODO: Replace with API call → GET /api/boarding/active-pets
// when backend is ready.

export type Session = "Sáng" | "Trưa" | "Chiều";
export type EatStatus = "Ăn tốt" | "Ăn ít" | "Bỏ ăn";
export type HygieneStatus = "Bình thường" | "Theo dõi thêm" | "Bất thường";

export interface LogEntry {
    session: Session;
    eat: EatStatus;
    hygiene: HygieneStatus;
    note: string;
    mediaCount: number;
    savedAt: string;
}

export interface BoardingPet {
    id: string;
    cage: string;
    petName: string;
    breed: string;
    ownerName: string;
    checkIn: string;
    checkOut: string;
    dayNum: number;
    totalDays: number;
    todayLogs: Session[];
}

export const MOCK_BOARDING_PETS: BoardingPet[] = [
    {
        id: "1",
        cage: "C12",
        petName: "Milu",
        breed: "Poodle",
        ownerName: "Nguyễn Văn A",
        checkIn: "22/05/2026",
        checkOut: "26/05/2026",
        dayNum: 3,
        totalDays: 4,
        todayLogs: ["Sáng"],
    },
    {
        id: "2",
        cage: "B03",
        petName: "Bơ",
        breed: "Corgi",
        ownerName: "Lê Thị D",
        checkIn: "23/05/2026",
        checkOut: "25/05/2026",
        dayNum: 2,
        totalDays: 2,
        todayLogs: [],
    },
    {
        id: "3",
        cage: "A08",
        petName: "Mít",
        breed: "Mèo ALN",
        ownerName: "Phạm Văn C",
        checkIn: "20/05/2026",
        checkOut: "25/05/2026",
        dayNum: 5,
        totalDays: 5,
        todayLogs: ["Sáng", "Trưa", "Chiều"],
    },
];

export const ALL_SESSIONS: Session[] = ["Sáng", "Trưa", "Chiều"];
export const EAT_OPTIONS: EatStatus[] = ["Ăn tốt", "Ăn ít", "Bỏ ăn"];
export const HYGIENE_OPTIONS: HygieneStatus[] = ["Bình thường", "Theo dõi thêm", "Bất thường"];
