export interface BoardingStay {
  petId: string;
  petName: string;
  speciesName: string;
  breedName?: string | null;
}

export interface StaffBoardingStay {
  sessionId: string;
  petId: string;
  petName: string;
  roomLabel: string;
  currentDay: number;
  totalDays: number;
  todayLogSummary: string;
}

export interface CareLogEntry {
  id: string;
  petId: string;
  petName: string;
  logDate: string;
  periodCode: string;
  periodLabel: string;
  feedingStatus: string;
  hygieneStatus: string;
  healthNote?: string | null;
  staffNote?: string | null;
  mediaCaptions: string[];
}

export interface UpsertCareLogPayload {
  sessionId: string;
  logDate?: string;
  periodCode: 'MORNING' | 'NOON' | 'AFTERNOON';
  feedingStatus: string;
  hygieneStatus: string;
  healthNote?: string;
  staffNote?: string;
}

export const BOARDING_PERIOD_OPTIONS = [
  { value: 'MORNING', label: 'Sáng' },
  { value: 'NOON', label: 'Trưa' },
  { value: 'AFTERNOON', label: 'Chiều' },
] as const;

export const BOARDING_FEEDING_OPTIONS = ['Ăn tốt', 'Ăn ít', 'Bỏ ăn'] as const;

export const BOARDING_HYGIENE_OPTIONS = ['Bình thường', 'Theo dõi thêm', 'Bất thường'] as const;
