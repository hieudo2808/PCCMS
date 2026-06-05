export interface BoardingStay {
  petId: string;
  petName: string;
  speciesName: string;
  breedName?: string | null;
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
