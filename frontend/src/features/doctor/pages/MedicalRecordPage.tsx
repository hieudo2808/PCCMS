import { useEffect } from "react";
import { useParams, Link } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { FormProvider, useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { toast } from "react-hot-toast";
import { Tag } from "~/components/atoms";
import { Card, EmptyState } from "~/components/molecules";
import { medicalRecordApi } from "~/shared/api/medicalRecordApi";
import { petApi } from "~/shared/api/petApi";
import { parseApiError } from "~/shared/utils/errorHandlers";
import { VitalSignsForm, vitalSignsSchema } from "../components/VitalSignsForm";

const fullRecordSchema = z.object({
    vitalSigns: vitalSignsSchema,
});

type FullRecordFormValues = z.infer<typeof fullRecordSchema>;


function formatAge(months?: number) {
    if (months == null) return "-";
    if (months < 12) return `${months} tháng`;
    const years = Math.floor(months / 12);
    const rest = months % 12;
    return rest ? `${years} năm ${rest} tháng` : `${years} năm`;
}

export function MedicalRecordPage() {
    const { id, appointmentId } = useParams<{ id?: string; appointmentId?: string }>();
    const queryClient = useQueryClient();

    const recordQuery = useQuery({
        queryKey: ["medicalRecord", id, appointmentId],
        queryFn: () => {
            if (id) return medicalRecordApi.getMedicalRecordById(id);
            if (appointmentId) return medicalRecordApi.getOrCreateMedicalRecordByAppointmentId(appointmentId);
            return Promise.reject(new Error("Thiếu mã bệnh án hoặc mã lịch hẹn"));
        },
        enabled: !!id || !!appointmentId,
    });

    const record = recordQuery.data;
    const recordId = record?.id;
    const isFinalized = record?.recordStatus === "FINALIZED";

    const petQuery = useQuery({
        queryKey: ["pet", record?.petId],
        queryFn: () => petApi.getPetById(record!.petId),
        enabled: !!record?.petId,
    });

    const prescriptionsQuery = useQuery({
        queryKey: ["medicalRecordPrescriptions", recordId],
        queryFn: () => medicalRecordApi.listPrescriptions(recordId!),
        enabled: !!recordId,
    });

    const methods = useForm<FullRecordFormValues>({
        resolver: zodResolver(fullRecordSchema) as any,
        defaultValues: {
            vitalSigns: {},
        },
    });

    useEffect(() => {
        if (!record) return;
        methods.reset({
            vitalSigns: {
                temperatureC: record.temperatureC,
                heartRateBpm: record.heartRateBpm,
                respiratoryRateBpm: record.respiratoryRateBpm,
                weightKg: record.weightKg,
                bloodPressure: record.bloodPressure,
                spo2Percent: record.spo2Percent,
                mucousMembraneColor: record.mucousMembraneColor,
                capillaryRefillSeconds: record.capillaryRefillSeconds,
                preliminaryDiagnosis: record.preliminaryDiagnosis,
                finalDiagnosis: record.finalDiagnosis,
                treatmentNote: record.treatmentNote,
            },
        });
    }, [record, methods]);

    const invalidateRecord = async () => {
        await Promise.all([
            queryClient.invalidateQueries({ queryKey: ["medicalRecord", id, appointmentId] }),
            queryClient.invalidateQueries({ queryKey: ["medicalRecordPrescriptions", recordId] }),
        ]);
    };

    const saveDraftMutation = useMutation({
        mutationFn: async (data: FullRecordFormValues) => {
            if (!recordId) throw new Error("Thiếu mã bệnh án");
            await medicalRecordApi.updateMedicalRecord(recordId, data.vitalSigns as any);
        },
        onSuccess: async () => {
            toast.success("Đã lưu nháp bệnh án");
            await invalidateRecord();
        },
        onError: (error) => toast.error(parseApiError(error)),
    });

    const finalizeMutation = useMutation({
        mutationFn: async (data: FullRecordFormValues) => {
            if (!recordId) throw new Error("Thiếu mã bệnh án");
            await medicalRecordApi.updateMedicalRecord(recordId, data.vitalSigns as any);
            await medicalRecordApi.finalizeMedicalRecord(recordId, {
                finalDiagnosis: data.vitalSigns.finalDiagnosis || "",
                treatmentNote: data.vitalSigns.treatmentNote,
            });
        },
        onSuccess: async () => {
            toast.success("Đã chốt bệnh án");
            await invalidateRecord();
        },
        onError: (error) => toast.error(parseApiError(error)),
    });

    if (recordQuery.isLoading) {
        return (
            <div className="flex items-center gap-2 p-8 text-sm text-slate-500">
                <span className="h-4 w-4 animate-spin rounded-full border-2 border-slate-300 border-t-primary-600" />
                Đang tải bệnh án...
            </div>
        );
    }

    if (recordQuery.isError || !record) {
        return <EmptyState title="Lỗi" description="Không thể tải bệnh án" />;
    }

    const pet = petQuery.data;
    const prescriptions = prescriptionsQuery.data ?? [];

    return (
        <FormProvider {...methods}>
            <div className="mb-4 flex flex-wrap items-center justify-between gap-4">
                <div className="flex items-center gap-3">
                    <h2 className="text-xl font-bold">Chi tiết bệnh án: {record.recordCode}</h2>
                    {isFinalized && <Tag tone="green">Đã chốt</Tag>}
                </div>
                {isFinalized && (
                    <Link
                        to={`/veterinarian/medical-records/${recordId}/prescriptions`}
                        className="rounded-lg bg-indigo-600 px-4 py-2 text-sm font-medium text-white hover:bg-indigo-700"
                    >
                        Kê đơn thuốc
                    </Link>
                )}
            </div>

            <div className="mb-6">
                <Card title="Thông tin thú cưng">
                    {petQuery.isLoading ? (
                        <p className="text-sm text-slate-500">Đang tải hồ sơ thú cưng...</p>
                    ) : !pet ? (
                        <p className="text-sm text-slate-500">Chưa tải được hồ sơ thú cưng.</p>
                    ) : (
                        <div className="grid gap-3 text-sm md:grid-cols-2 xl:grid-cols-4">
                            <div><span className="text-slate-500">Tên:</span> <span className="font-medium">{pet.name}</span></div>
                            <div><span className="text-slate-500">Loài:</span> <span className="font-medium">{pet.speciesName || "-"}</span></div>
                            <div><span className="text-slate-500">Giống:</span> <span className="font-medium">{pet.breedName || "-"}</span></div>
                            <div><span className="text-slate-500">Giới tính:</span> <span className="font-medium">{pet.sex}</span></div>
                            <div><span className="text-slate-500">Tuổi:</span> <span className="font-medium">{formatAge(pet.estimatedAgeMonths)}</span></div>
                            <div><span className="text-slate-500">Cân nặng hồ sơ:</span> <span className="font-medium">{pet.weightKg ? `${pet.weightKg} kg` : "-"}</span></div>
                            <div className="md:col-span-2"><span className="text-slate-500">Dị ứng:</span> <span className="font-medium">{pet.allergyNote || "-"}</span></div>
                            <div className="md:col-span-2"><span className="text-slate-500">Ghi chú chăm sóc:</span> <span className="font-medium">{pet.specialNote || pet.nutritionNote || "-"}</span></div>
                        </div>
                    )}
                </Card>
            </div>

            <div className="grid gap-6 xl:grid-cols-[1.15fr_0.85fr]">
                <VitalSignsForm
                    disabled={isFinalized}
                    initialData={methods.getValues().vitalSigns}
                    onSaveDraft={(data) => {
                        methods.setValue("vitalSigns", data);
                        saveDraftMutation.mutate(methods.getValues());
                    }}
                    onFinalize={(data) => {
                        methods.setValue("vitalSigns", data);
                        finalizeMutation.mutate(methods.getValues());
                    }}
                    isSaving={saveDraftMutation.isPending}
                    isFinalizing={finalizeMutation.isPending}
                />

                <div className="space-y-6">
                    <Card title="Đơn thuốc đã tạo">
                        {prescriptions.length === 0 ? (
                            <p className="text-sm text-slate-500">Chưa có đơn thuốc.</p>
                        ) : (
                            <div className="space-y-4">
                                {prescriptions.map((prescription) => (
                                    <div key={prescription.id} className="rounded-md border border-slate-200 p-3">
                                        <div className="mb-2 flex items-center justify-between gap-3">
                                            <div className="font-medium text-slate-900">{prescription.prescriptionCode}</div>
                                            <div className="text-xs text-slate-500">{new Date(prescription.issuedAt).toLocaleString("vi-VN")}</div>
                                        </div>
                                        <div className="space-y-2">
                                            {prescription.items.map((item) => (
                                                <div key={item.id} className="text-sm text-slate-700">
                                                    <span className="font-medium">{item.medicineName || item.medicineId}</span>
                                                    {item.medicineUnit && <span> ({item.medicineUnit})</span>}
                                                    <span> - SL {item.quantity}</span>
                                                    {item.dosage && <span> - {item.dosage}</span>}
                                                    {item.instruction && <span> - {item.instruction}</span>}
                                                </div>
                                            ))}
                                        </div>
                                    </div>
                                ))}
                            </div>
                        )}
                    </Card>
                </div>
            </div>
        </FormProvider>
    );
}
