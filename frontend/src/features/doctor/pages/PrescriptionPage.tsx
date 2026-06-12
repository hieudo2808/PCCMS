import { useParams, useNavigate } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { FormProvider, useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { toast } from "react-hot-toast";
import { Button } from "~/components/atoms";
import { Card, EmptyState } from "~/components/molecules";
import { medicalRecordApi } from "~/shared/api/medicalRecordApi";
import { parseApiError } from "~/shared/utils/errorHandlers";
import { PrescriptionTable, prescriptionFormSchema, type PrescriptionFormValues } from "../components/PrescriptionTable";

export function PrescriptionPage() {
    const { id } = useParams<{ id: string }>();
    const navigate = useNavigate();
    const queryClient = useQueryClient();

    const recordQuery = useQuery({
        queryKey: ["medicalRecord", id],
        queryFn: () => medicalRecordApi.getMedicalRecordById(id!),
        enabled: !!id,
    });

    const prescriptionsQuery = useQuery({
        queryKey: ["medicalRecordPrescriptions", id],
        queryFn: () => medicalRecordApi.listPrescriptions(id!),
        enabled: !!id,
    });

    const methods = useForm<PrescriptionFormValues>({
        resolver: zodResolver(prescriptionFormSchema) as any,
        defaultValues: {
            items: [],
        },
    });

    const createMutation = useMutation({
        mutationFn: async (data: PrescriptionFormValues) => {
            if (!id) throw new Error("Thiếu mã bệnh án");
            
            const payload = {
                items: data.items
                    .filter((item) => item.medicineId && item.quantity > 0)
                    .map((item) => ({
                        medicineId: item.medicineId,
                        dosage: item.dosage || "",
                        quantity: item.quantity,
                        instruction: item.instruction || "",
                    })),
            };

            if (payload.items.length === 0) {
                throw new Error("Vui lòng kê ít nhất một loại thuốc hợp lệ");
            }

            await medicalRecordApi.createPrescription(id, payload);
        },
        onSuccess: async () => {
            methods.setValue("items", []);
            toast.success("Đã kê đơn thuốc thành công");
            await queryClient.invalidateQueries({ queryKey: ["medicalRecordPrescriptions", id] });
        },
        onError: (error) => toast.error(parseApiError(error)),
    });

    const record = recordQuery.data;
    const isFinalized = record?.recordStatus === "FINALIZED";

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

    const prescriptions = prescriptionsQuery.data ?? [];

    return (
        <FormProvider {...methods}>
            <div className="mb-4 flex items-center justify-between">
                <div>
                    <h2 className="text-xl font-bold">Kê đơn thuốc: {record.recordCode}</h2>
                    <p className="text-sm text-slate-500 mt-1">
                        Thú cưng: <span className="font-medium text-slate-700">{record.petName || record.petId}</span> -
                        Chẩn đoán: <span className="font-medium text-slate-700">{record.finalDiagnosis || record.preliminaryDiagnosis || "Chưa có"}</span>
                    </p>
                </div>
                <Button variant="outline" onClick={() => navigate(`/veterinarian/medical-records/${id}`)}>Quay lại bệnh án</Button>
            </div>

            <div className="grid gap-6 xl:grid-cols-[1fr_350px]">
                <div className="space-y-6">
                    <PrescriptionTable disabled={!isFinalized} />
                    
                    <div className="flex justify-end gap-3">
                        <Button 
                            variant="primary" 
                            disabled={createMutation.isPending || !isFinalized}
                            onClick={methods.handleSubmit((data) => createMutation.mutate(data))}
                        >
                            {createMutation.isPending ? "Đang xử lý..." : "Lưu đơn thuốc"}
                        </Button>
                    </div>

                    {!isFinalized && (
                        <p className="text-sm text-red-500 text-right">Bệnh án phải được chốt trước khi kê đơn.</p>
                    )}
                </div>

                <div className="space-y-6">
                    <Card title="Lịch sử đơn thuốc">
                        {prescriptionsQuery.isLoading ? (
                            <p className="text-sm text-slate-500">Đang tải...</p>
                        ) : prescriptions.length === 0 ? (
                            <p className="text-sm text-slate-500">Chưa có đơn thuốc nào.</p>
                        ) : (
                            <div className="space-y-4">
                                {prescriptions.map((prescription) => (
                                    <div key={prescription.id} className="rounded-md border border-slate-200 p-3 bg-white shadow-sm">
                                        <div className="mb-2 flex items-center justify-between gap-3 border-b border-slate-100 pb-2">
                                            <div className="font-semibold text-slate-900">{prescription.prescriptionCode}</div>
                                            <div className="text-xs text-slate-500">{new Date(prescription.issuedAt).toLocaleString("vi-VN")}</div>
                                        </div>
                                        <div className="space-y-2">
                                            {prescription.items.map((item) => (
                                                <div key={item.id} className="text-sm text-slate-700">
                                                    <span className="font-medium">{item.medicineName || item.medicineId}</span>
                                                    {item.medicineUnit && <span> ({item.medicineUnit})</span>}
                                                    <div className="text-xs text-slate-500 mt-0.5">
                                                        SL: <span className="font-medium text-slate-700">{item.quantity}</span>
                                                        {item.dosage && <span> • Mẫu: {item.dosage}</span>}
                                                    </div>
                                                    {item.instruction && <div className="text-xs italic text-slate-600 mt-0.5">"{item.instruction}"</div>}
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
