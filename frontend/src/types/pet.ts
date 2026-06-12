import type { HealthAlertResponse } from "./medicalRecord";

export type PetSex = "MALE" | "FEMALE" | "UNKNOWN";

/** Hồ sơ nền thú cưng — không gồm bản ghi y tế theo lần khám */
export interface PetResponse {
    id: string;
    ownerId: string;
    name: string;
    speciesId: string;
    speciesName?: string;
    breedId: string;
    breedName?: string;
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

export interface PetSpeciesOption { [key: string]: any; }
export interface PetBreedOption { [key: string]: any; }
