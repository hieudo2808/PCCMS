import axiosClient from '~/shared/api/axiosClient';
import type { PetBreedOption, PetSpeciesOption } from '~/types/pet';

export const petCatalogApi = {
  getSpecies: (): Promise<PetSpeciesOption[]> => {
    return axiosClient.get('/v1/pet-catalog/species');
  },

  getBreedsBySpecies: (speciesId: string): Promise<PetBreedOption[]> => {
    return axiosClient.get(`/v1/pet-catalog/species/${speciesId}/breeds`);
  },
};
