import { describe, it, expect, vi } from "vitest";
import { petApi } from "~/shared/api/petApi";
import axiosClient from "~/shared/api/axiosClient";

vi.mock("~/shared/api/axiosClient");

const mockPet = {
    id: "pet-123",
    ownerId: "owner-123",
    name: "Milu",
    speciesId: "species-123",
    breedId: "breed-123",
    sex: "MALE" as const,
    birthDate: "2023-01-01",
    estimatedAgeMonths: 24,
    weightKg: 5,
    color: "Brown",
    identificationNote: "",
    specialNote: "",
    allergyNote: "",
    nutritionNote: "",
    isActive: true,
    healthAlerts: [],
};

describe("petApi", () => {
    it("gets pets successfully", async () => {
        const mockPage = {
            content: [mockPet],
            pageNumber: 1,
            pageSize: 20,
            totalElements: 1,
            totalPages: 1,
            isLast: true,
        };
        vi.mocked(axiosClient.get).mockResolvedValueOnce(mockPage);

        const result = await petApi.getPets();
        expect(result).toEqual(mockPage);
        expect(axiosClient.get).toHaveBeenCalledWith("/v1/pets", { params: undefined });
    });

    it("gets pet by id successfully", async () => {
        vi.mocked(axiosClient.get).mockResolvedValueOnce(mockPet);

        const result = await petApi.getPetById("pet-123");
        expect(result).toEqual(mockPet);
        expect(axiosClient.get).toHaveBeenCalledWith("/v1/pets/pet-123");
    });

    it("creates pet successfully", async () => {
        vi.mocked(axiosClient.post).mockResolvedValueOnce(mockPet);

        const request = { name: "Milu", sex: "MALE" as const, speciesId: "species-1" };
        const result = await petApi.createPet(request);
        expect(result).toEqual(mockPet);
        expect(axiosClient.post).toHaveBeenCalledWith("/v1/pets", request);
    });

    it("updates pet successfully", async () => {
        vi.mocked(axiosClient.put).mockResolvedValueOnce(mockPet);

        const request = { name: "Milu 2", sex: "MALE" as const, speciesId: "species-1" };
        const result = await petApi.updatePet("pet-123", request);
        expect(result).toEqual(mockPet);
        expect(axiosClient.put).toHaveBeenCalledWith("/v1/pets/pet-123", request);
    });

    it("deletes pet successfully", async () => {
        vi.mocked(axiosClient.delete).mockResolvedValueOnce(undefined);
        await petApi.deletePet("pet-123");
        expect(axiosClient.delete).toHaveBeenCalledWith("/v1/pets/pet-123");
    });
});
