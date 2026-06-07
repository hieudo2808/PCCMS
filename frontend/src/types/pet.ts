import type { HealthAlertResponse } from "./medicalRecord";

export type PetSex = "MALE" | "FEMALE" | "UNKNOWN";

export interface PetResponse {
    id: string;
    ownerId: string;
    name: string;
    speciesId: string;
    breedId: string;
    sex: PetSex;
    birthDate: string;
    estimatedAgeMonths: number;
    weightKg: number;
    color: string;
    identificationNote: string;
    specialNote: string;
    allergyNote: string;
    nutritionNote: string;
    isActive: boolean;
    healthAlerts: HealthAlertResponse[];
}

export interface PetRequest {
    name: string;
    speciesId: string;
    breedId?: string;
    sex: PetSex;
    birthDate?: string;
    weightKg?: number;
    color?: string;
    identificationNote?: string;
    specialNote?: string;
    allergyNote?: string;
    nutritionNote?: string;
}
