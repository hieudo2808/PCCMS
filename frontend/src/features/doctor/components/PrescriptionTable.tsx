import { useFieldArray, useFormContext, useWatch } from "react-hook-form";
import { Plus, Trash2 } from "lucide-react";
import { Button, Input, Textarea } from "~/components/atoms";
import { Card, DataTable } from "~/components/molecules";
import { z } from "zod";
import { useEffect } from "react";

export const prescriptionItemSchema = z.object({
    medicineId: z.string().min(1, "Vui lòng chọn thuốc"),
    dosage: z.coerce.number().min(0).optional(),
    frequency: z.coerce.number().min(0).optional(),
    durationDays: z.coerce.number().min(0).optional(),
    quantity: z.coerce.number().min(1, "Số lượng phải >= 1"),
    dosageInstruction: z.string().min(1, "Vui lòng nhập hướng dẫn"),
});

export const prescriptionFormSchema = z.object({
    items: z.array(prescriptionItemSchema),
});

export type PrescriptionFormValues = z.infer<typeof prescriptionFormSchema>;

interface PrescriptionTableProps {
    disabled?: boolean;
}

export function PrescriptionTable({ disabled = false }: PrescriptionTableProps) {
    const {
        control,
        register,
        setValue,
        formState: { errors },
    } = useFormContext<PrescriptionFormValues>();

    const { fields, append, remove } = useFieldArray({
        control,
        name: "items",
    });

    const watchItems =
        useWatch({
            control,
            name: "items",
        }) || [];

    // Auto calculate total quantity
    useEffect(() => {
        watchItems.forEach((item, index) => {
            const dosage = Number(item.dosage);
            const frequency = Number(item.frequency);
            const durationDays = Number(item.durationDays);

            if (dosage > 0 && frequency > 0 && durationDays > 0) {
                const calculated = dosage * frequency * durationDays;
                if (calculated !== item.quantity) {
                    setValue(`items.${index}.quantity`, calculated);
                }
            }
        });
    }, [watchItems, setValue]);

    return (
        <Card title="Kê đơn thuốc">
            <div className="space-y-6">
                <DataTable
                    columns={["Thuốc", "Liều x Lần x Ngày", "Tổng số lượng", "Hướng dẫn", ""]}
                    rows={fields.map((field, index) => {
                        const itemErrors = errors.items?.[index];
                        return [
                            <div key={`medicine-${field.id}`} className="w-48">
                                <Input
                                    placeholder="ID Thuốc (VD: Amoxicillin)"
                                    disabled={disabled}
                                    {...register(`items.${index}.medicineId`)}
                                    error={itemErrors?.medicineId?.message}
                                />
                            </div>,
                            <div key={`calc-${field.id}`} className="flex items-center gap-2">
                                <Input
                                    type="number"
                                    className="w-16"
                                    placeholder="Liều"
                                    disabled={disabled}
                                    {...register(`items.${index}.dosage`, { valueAsNumber: true })}
                                />
                                <span>x</span>
                                <Input
                                    type="number"
                                    className="w-16"
                                    placeholder="Lần"
                                    disabled={disabled}
                                    {...register(`items.${index}.frequency`, {
                                        valueAsNumber: true,
                                    })}
                                />
                                <span>x</span>
                                <Input
                                    type="number"
                                    className="w-16"
                                    placeholder="Ngày"
                                    disabled={disabled}
                                    {...register(`items.${index}.durationDays`, {
                                        valueAsNumber: true,
                                    })}
                                />
                            </div>,
                            <div key={`quantity-${field.id}`} className="w-24">
                                <Input
                                    type="number"
                                    disabled={disabled}
                                    {...register(`items.${index}.quantity`, {
                                        valueAsNumber: true,
                                    })}
                                    error={itemErrors?.quantity?.message}
                                />
                            </div>,
                            <div key={`instruction-${field.id}`} className="w-48">
                                <Textarea
                                    rows={2}
                                    disabled={disabled}
                                    {...register(`items.${index}.dosageInstruction`)}
                                    error={itemErrors?.dosageInstruction?.message}
                                />
                            </div>,
                            <div key={`action-${field.id}`} className="flex justify-center">
                                <Button
                                    type="button"
                                    variant="ghost"
                                    disabled={disabled}
                                    onClick={() => remove(index)}
                                    className="text-red-500 hover:text-red-700 hover:bg-red-50 p-2 h-auto"
                                >
                                    <Trash2 className="h-4 w-4" />
                                </Button>
                            </div>,
                        ];
                    })}
                />

                {!disabled && (
                    <Button
                        type="button"
                        variant="outline"
                        className="w-full border-dashed"
                        onClick={() =>
                            append({
                                medicineId: "",
                                quantity: 0,
                                dosageInstruction: "",
                                dosage: 0,
                                frequency: 0,
                                durationDays: 0,
                            })
                        }
                    >
                        <Plus className="h-4 w-4 mr-2" /> Thêm thuốc
                    </Button>
                )}
            </div>
        </Card>
    );
}
