import { useEffect, useState } from "react";
import { useFieldArray, useFormContext, useWatch } from "react-hook-form";
import { Plus, Trash2 } from "lucide-react";
import { z } from "zod";
import { Button, Input, Textarea } from "~/components/atoms";
import { Card, DataTable } from "~/components/molecules";
import { medicineApi, type MedicineSuggestion } from "~/shared/api/medicineApi";

export const prescriptionItemSchema = z.object({
    medicineId: z.string().min(1, "Vui lòng chọn thuốc từ danh sách"),
    medicineName: z.string().optional(),
    dosage: z.string().optional(),
    quantity: z.coerce.number().min(1, "Số lượng phải >= 1"),
    instruction: z.string().optional(),
});

export const prescriptionFormSchema = z.object({
    items: z.array(prescriptionItemSchema),
});

export type PrescriptionFormValues = z.infer<typeof prescriptionFormSchema>;

interface PrescriptionTableProps {
    disabled?: boolean;
}

function MedicinePicker({
    index,
    disabled,
    error,
}: {
    index: number;
    disabled: boolean;
    error?: string;
}) {
    const { register, setValue, getValues } = useFormContext<any>();
    const selectedName = useWatch({ name: `prescription.items.${index}.medicineName` }) || "";
    const [keyword, setKeyword] = useState(selectedName);
    const [open, setOpen] = useState(false);
    const [suggestions, setSuggestions] = useState<MedicineSuggestion[]>([]);
    const [isLoadingSuggestions, setIsLoadingSuggestions] = useState(false);
    useEffect(() => {
        setKeyword(selectedName);
    }, [selectedName]);

    useEffect(() => {
        const trimmed = keyword.trim();
        if (!open || disabled || trimmed.length < 1) {
            setSuggestions([]);
            setIsLoadingSuggestions(false);
            return;
        }

        let cancelled = false;
        setIsLoadingSuggestions(true);
        medicineApi
            .suggestMedicines(trimmed)
            .then((items) => {
                if (!cancelled) {
                    setSuggestions(items);
                }
            })
            .catch(() => {
                if (!cancelled) {
                    setSuggestions([]);
                }
            })
            .finally(() => {
                if (!cancelled) {
                    setIsLoadingSuggestions(false);
                }
            });

        return () => {
            cancelled = true;
        };
    }, [disabled, keyword, open]);

    const selectMedicine = (medicine: MedicineSuggestion) => {
        const firstInstruction = (medicine.defaultInstruction || "")
            .split(/\r?\n/)
            .map((line) => line.trim())
            .find(Boolean);
        setValue(`items.${index}.medicineId`, medicine.id, { shouldValidate: true, shouldDirty: true });
        setValue(`items.${index}.medicineName`, medicine.name, { shouldValidate: true, shouldDirty: true });
        if (!getValues(`items.${index}.instruction`) && firstInstruction) {
            setValue(`items.${index}.instruction`, firstInstruction, { shouldValidate: true, shouldDirty: true });
        }
        setKeyword(medicine.name);
        setOpen(false);
    };

    return (
        <div className={`relative w-72 ${open ? "z-50" : "z-10"}`}>
            <input type="hidden" {...register(`items.${index}.medicineId`)} />
            <Input
                value={keyword}
                placeholder="Nhập tên thuốc"
                disabled={disabled}
                onFocus={() => setOpen(true)}
                onChange={(event) => {
                    setKeyword(event.target.value);
                    setOpen(true);
                    setValue(`items.${index}.medicineId`, "", { shouldValidate: true, shouldDirty: true });
                    setValue(`items.${index}.medicineName`, event.target.value, { shouldDirty: true });
                }}
                error={error}
            />
            {open && !disabled && (
                <div className="absolute z-30 mt-1 max-h-72 w-full overflow-auto rounded-lg border border-slate-200 bg-white shadow-lg">
                    {isLoadingSuggestions ? (
                        <div className="px-3 py-2 text-sm text-slate-500">Đang tìm thuốc...</div>
                    ) : suggestions.length === 0 ? (
                        <div className="px-3 py-2 text-sm text-slate-500">Không có thuốc phù hợp</div>
                    ) : (
                        suggestions.map((medicine) => (
                            <button
                                key={medicine.id}
                                type="button"
                                className="block w-full px-3 py-2 text-left text-sm hover:bg-slate-50 focus:bg-slate-50"
                                onMouseDown={(event) => event.preventDefault()}
                                onClick={() => selectMedicine(medicine)}
                            >
                                <span className="block font-medium text-slate-900">{medicine.name}</span>
                                <span className="block text-xs text-slate-500">
                                    {medicine.categoryName || "Chưa phân nhóm"} - {medicine.unit} - tồn {medicine.currentStock}
                                </span>
                            </button>
                        ))
                    )}
                </div>
            )}
            
        </div>
    );
}
export function useMedicineTemplates(medicineId?: string) {
    const [templates, setTemplates] = useState<any[]>([]);
    useEffect(() => {
        let cancelled = false;
        if (medicineId) {
            import('~/features/admin/medicine-management/medicineService')
                .then(m => m.getMedicineUsageTemplates(medicineId))
                .then(data => {
                    if (!cancelled) setTemplates(data || []);
                })
                .catch(() => {
                    if (!cancelled) setTemplates([]);
                });
        } else {
            setTemplates([]);
        }
        return () => { cancelled = true; };
    }, [medicineId]);
    return templates;
}

function TemplateDosageInput({ index, disabled, error }: { index: number, disabled: boolean, error?: string }) {
    const { register, setValue } = useFormContext<any>();
    const medicineId = useWatch({ name: `items.${index}.medicineId` });
    const templates = useMedicineTemplates(medicineId);
    
    return (
        <div className="space-y-1 w-40">
            <Input disabled={disabled} placeholder="VD: 2 viên..." {...register(`items.${index}.dosage`)} error={error} />
            {templates.length > 0 && !disabled && (
                <select
                    className="w-full rounded-lg border border-slate-300 bg-white px-2 py-1 text-xs text-slate-700 focus:border-indigo-500 focus:outline-none"
                    onChange={(e) => {
                        if (!e.target.value) return;
                        const t = templates.find(x => x.id === e.target.value);
                        if (t && t.dosage) {
                            setValue(`items.${index}.dosage`, t.dosage, { shouldValidate: true, shouldDirty: true });
                        }
                        e.target.value = "";
                    }}
                >
                    <option value="">-- Gợi ý --</option>
                    {templates.filter(t => t.dosage).map(t => (
                        <option key={t.id} value={t.id}>{t.label}: {t.dosage}</option>
                    ))}
                </select>
            )}
        </div>
    );
}

function TemplateInstructionInput({ index, disabled, error }: { index: number, disabled: boolean, error?: string }) {
    const { register, setValue } = useFormContext<any>();
    const medicineId = useWatch({ name: `items.${index}.medicineId` });
    const templates = useMedicineTemplates(medicineId);
    
    return (
        <div className="space-y-1 w-64">
            <Textarea rows={2} disabled={disabled} placeholder="Không bắt buộc..." {...register(`items.${index}.instruction`)} error={error} />
            {templates.length > 0 && !disabled && (
                <select
                    className="w-full rounded-lg border border-slate-300 bg-white px-2 py-1 text-xs text-slate-700 focus:border-indigo-500 focus:outline-none"
                    onChange={(e) => {
                        if (!e.target.value) return;
                        const t = templates.find(x => x.id === e.target.value);
                        if (t && t.instruction) {
                            setValue(`items.${index}.instruction`, t.instruction, { shouldValidate: true, shouldDirty: true });
                        }
                        e.target.value = "";
                    }}
                >
                    <option value="">-- Chọn hướng dẫn --</option>
                    {templates.filter(t => t.instruction).map(t => (
                        <option key={t.id} value={t.id}>{t.label}</option>
                    ))}
                </select>
            )}
        </div>
    );
}

export function PrescriptionTable({ disabled = false }: PrescriptionTableProps) {
    const {
        control,
        register,
        formState: { errors },
    } = useFormContext<any>();

    const { fields, append, remove } = useFieldArray({
        control,
        name: "items",
    });



    return (
        <Card title="Kê đơn thuốc">
            <div className="space-y-6">
                <DataTable
                    overflowVisible
                    columns={["Thuốc", "Số lượng", "Mẫu Liều", "Hướng dẫn", ""]}
                    rows={fields.map((field, index) => {
                        const itemErrors = (errors.items as any)?.[index];
                        return [
                            <MedicinePicker
                                key={`medicine-${field.id}`}
                                index={index}
                                disabled={disabled}
                                error={itemErrors?.medicineId?.message}
                            />,
                            <div key={`quantity-${field.id}`} className="w-24">
                                <Input type="number" min="1" disabled={disabled} {...register(`items.${index}.quantity`, { valueAsNumber: true })} error={itemErrors?.quantity?.message} />
                            </div>,
                            <TemplateDosageInput
                                key={`dosage-${field.id}`}
                                index={index}
                                disabled={disabled}
                                error={itemErrors?.dosage?.message}
                            />,
                            <TemplateInstructionInput
                                key={`instruction-${field.id}`}
                                index={index}
                                disabled={disabled}
                                error={itemErrors?.instruction?.message}
                            />,
                            <div key={`action-${field.id}`} className="flex justify-center">
                                <Button type="button" variant="ghost" disabled={disabled} onClick={() => remove(index)} className="h-auto p-2 text-red-500 hover:bg-red-50 hover:text-red-700">
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
                                medicineName: "",
                                quantity: 1,
                                instruction: "",
                                dosage: "",
                            })
                        }
                    >
                        <Plus className="mr-2 h-4 w-4" /> Thêm thuốc
                    </Button>
                )}
            </div>
        </Card>
    );
}
