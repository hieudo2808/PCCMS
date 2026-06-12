import { describe, it, expect, vi } from "vitest";
import { medicalRecordApi } from "~/shared/api/medicalRecordApi";
import axiosClient from "~/shared/api/axiosClient";

vi.mock("~/shared/api/axiosClient");

const mockRecord = {
    id: "mr-1",
    recordCode: "MR-001",
    appointmentId: "app-1",
    petId: "pet-1",
    vetId: "vet-1",
    recordStatus: "DRAFT" as const,
    temperatureC: 38.5,
    heartRateBpm: 120,
    respiratoryRateBpm: 30,
    weightKg: 5,
    bloodPressure: "120/80",
    spo2Percent: 98,
    mucousMembraneColor: "Pink",
    capillaryRefillSeconds: 2,
    preliminaryDiagnosis: "Healthy",
    finalDiagnosis: "",
    treatmentNote: "",
    followUpAt: "",
    lockedAt: "",
    createdAt: "2026-06-03",
    updatedAt: "2026-06-03",
};

describe("medicalRecordApi", () => {
    it("gets medical record by id successfully", async () => {
        vi.mocked(axiosClient.get).mockResolvedValueOnce(mockRecord);

        const result = await medicalRecordApi.getMedicalRecordById("mr-1");
        expect(result).toEqual(mockRecord);
        expect(axiosClient.get).toHaveBeenCalledWith("/v1/medical-records/mr-1");
    });

    it("updates medical record successfully", async () => {
        vi.mocked(axiosClient.put).mockResolvedValueOnce(mockRecord);

        const data = { temperatureC: 39 };
        const result = await medicalRecordApi.updateMedicalRecord("mr-1", data);
        expect(result).toEqual(mockRecord);
        expect(axiosClient.put).toHaveBeenCalledWith("/v1/medical-records/mr-1", data);
    });

    it("finalizes medical record successfully", async () => {
        vi.mocked(axiosClient.patch).mockResolvedValueOnce(mockRecord);

        const data = { finalDiagnosis: "Cured" };
        const result = await medicalRecordApi.finalizeMedicalRecord("mr-1", data);
        expect(result).toEqual(mockRecord);
        expect(axiosClient.patch).toHaveBeenCalledWith("/v1/medical-records/mr-1/finalize", data);
    });

    it("creates prescription successfully", async () => {
        const mockPrescription = {
            id: "pres-1",
            prescriptionCode: "PRE-001",
            medicalRecordId: "mr-1",
            vetId: "vet-1",
            issuedAt: "2026-06-03T08:00:00Z",
            items: [],
        };
        vi.mocked(axiosClient.post).mockResolvedValueOnce(mockPrescription);

        const data = {
            items: [{ medicineId: "med-1", quantity: 10, dosage: "2", instruction: "2 viên/ngày" }],
        };
        const result = await medicalRecordApi.createPrescription("mr-1", data);
        expect(result).toEqual(mockPrescription);
        expect(axiosClient.post).toHaveBeenCalledWith(
            "/v1/medical-records/mr-1/prescriptions",
            data
        );
    });

    it("lists prescriptions successfully", async () => {
        vi.mocked(axiosClient.get).mockResolvedValueOnce([]);

        const result = await medicalRecordApi.listPrescriptions("mr-1");

        expect(result).toEqual([]);
        expect(axiosClient.get).toHaveBeenCalledWith("/v1/medical-records/mr-1/prescriptions");
    });
});
