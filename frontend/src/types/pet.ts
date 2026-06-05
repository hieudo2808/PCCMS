export type PetSex = 'MALE' | 'FEMALE' | 'UNKNOWN';

/** Hồ sơ nền thú cưng — không gồm bản ghi y tế theo lần khám */
export interface PetResponse {
  id: string;
  ownerId: string;
  name: string;
  speciesId: string;
  speciesName: string;
  breedId?: string;
  breedName?: string;
  sex: PetSex;
  birthDate?: string;
  estimatedAgeMonths?: number;
  weightKg?: number;
  color?: string;
  identificationNote?: string;
  specialNote?: string;
  allergyNote?: string;
  nutritionNote?: string;
  isActive: boolean;
}

export interface PetSpeciesOption {
  id: string;
  name: string;
}

export interface PetBreedOption {
  id: string;
  speciesId: string;
  name: string;
}

export interface PetRequest {
  ownerId?: string;
  name: string;
  speciesId: string;
  breedId?: string;
  sex: PetSex;
  birthDate?: string;
  estimatedAgeMonths?: number;
  weightKg?: number;
  color?: string;
  identificationNote?: string;
  specialNote?: string;
  allergyNote?: string;
  nutritionNote?: string;
}
