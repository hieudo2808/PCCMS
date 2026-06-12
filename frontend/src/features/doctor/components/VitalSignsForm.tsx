import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Button, Input, Textarea } from "~/components/atoms";
import { Card } from "~/components/molecules";

const optionalNonNegativeNumber = (message: string) =>
    z.coerce.number().min(0, message).optional().or(z.literal(""));

export const vitalSignsSchema = z.object({
    temperatureC: optionalNonNegativeNumber("Nhiệt độ không được âm"),
    heartRateBpm: optionalNonNegativeNumber("Nhịp tim không được âm"),
    respiratoryRateBpm: optionalNonNegativeNumber("Nhịp thở không được âm"),
    weightKg: optionalNonNegativeNumber("Cân nặng không được âm"),
    bloodPressure: z.string().optional(),
    spo2Percent: z.coerce.number().min(0, "SpO2 >= 0").max(100, "SpO2 <= 100").optional().or(z.literal("")),
    mucousMembraneColor: z.string().optional(),
    capillaryRefillSeconds: optionalNonNegativeNumber("CRT không được âm"),
    preliminaryDiagnosis: z.string().optional(),
    finalDiagnosis: z.string().optional(),
    treatmentNote: z.string().optional(),
});

export type VitalSignsFormValues = z.infer<typeof vitalSignsSchema>;

interface VitalSignsFormProps {
    initialData?: Partial<VitalSignsFormValues>;
    disabled?: boolean;
    onSaveDraft?: (data: VitalSignsFormValues) => void;
    onFinalize?: (data: VitalSignsFormValues) => void;
    isSaving?: boolean;
    isFinalizing?: boolean;
}

export function VitalSignsForm({
    initialData,
    disabled = false,
    onSaveDraft,
    onFinalize,
    isSaving = false,
    isFinalizing = false,
}: VitalSignsFormProps) {
    const {
        register,
        handleSubmit,
        reset,
        formState: { errors },
    } = useForm<VitalSignsFormValues>({
        resolver: zodResolver(vitalSignsSchema) as any,
        defaultValues: initialData,
    });

    useEffect(() => {
        if (initialData) reset(initialData);
    }, [initialData, reset]);

    return (
        <Card title="Nhập bệnh án">
            <form>
                <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
                    <Input label="Nhiệt độ (°C)" type="number" step="0.1" min="0" disabled={disabled} {...register("temperatureC")} error={errors.temperatureC?.message} />
                    <Input label="Nhịp tim" type="number" min="0" disabled={disabled} {...register("heartRateBpm")} error={errors.heartRateBpm?.message} />
                    <Input label="Nhịp thở" type="number" min="0" disabled={disabled} {...register("respiratoryRateBpm")} error={errors.respiratoryRateBpm?.message} />
                    <Input label="Cân nặng (kg)" type="number" step="0.1" min="0" disabled={disabled} {...register("weightKg")} error={errors.weightKg?.message} />
                    <Input label="Huyết áp" disabled={disabled} {...register("bloodPressure")} error={errors.bloodPressure?.message} />
                    <Input label="SpO2 (%)" type="number" min="0" max="100" disabled={disabled} {...register("spo2Percent")} error={errors.spo2Percent?.message} />
                    <Input label="Màu niêm mạc" disabled={disabled} {...register("mucousMembraneColor")} error={errors.mucousMembraneColor?.message} />
                    <Input label="CRT (giây)" type="number" step="0.1" min="0" disabled={disabled} {...register("capillaryRefillSeconds")} error={errors.capillaryRefillSeconds?.message} />
                </div>

                <div className="mt-4 grid gap-4">
                    <Textarea label="Chẩn đoán ban đầu" rows={3} disabled={disabled} {...register("preliminaryDiagnosis")} error={errors.preliminaryDiagnosis?.message} />
                    <Textarea label="Chẩn đoán xác định" rows={3} disabled={disabled} {...register("finalDiagnosis")} error={errors.finalDiagnosis?.message} />
                    <Textarea label="Ghi chú điều trị" rows={3} disabled={disabled} {...register("treatmentNote")} error={errors.treatmentNote?.message} />
                </div>

                {!disabled && (
                    <div className="mt-6 flex gap-2">
                        <Button type="button" onClick={handleSubmit((data) => onSaveDraft?.(data))} disabled={isSaving || isFinalizing}>
                            {isSaving ? "Đang lưu..." : "Lưu nháp"}
                        </Button>
                        <Button variant="secondary" type="button" onClick={handleSubmit((data) => onFinalize?.(data))} disabled={isSaving || isFinalizing}>
                            {isFinalizing ? "Đang chốt..." : "Chốt bệnh án"}
                        </Button>
                    </div>
                )}
            </form>
        </Card>
    );
}
