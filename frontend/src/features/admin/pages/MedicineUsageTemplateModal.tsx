import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { toast } from "react-hot-toast";
import { z } from "zod";
import { Button } from "~/components/atoms/Button";
import { Input } from "~/components/atoms/Input";
import { Modal } from "~/components/molecules/Modal";
import { DataTable } from "~/components/molecules/DataTable";
import { EmptyState } from "~/components/molecules/EmptyState";
import { Tag } from "~/components/atoms/Tag";
import type { MedicineUsageTemplate } from "~/features/admin/medicine-management/medicineService";
import {
    createMedicineUsageTemplate,
    deleteMedicineUsageTemplate,
    getMedicineUsageTemplates,
    updateMedicineUsageTemplate
} from "~/features/admin/medicine-management/medicineService";
import type { Medicine } from "~/features/admin/medicine-management/types";

const templateSchema = z.object({
    label: z.string().min(1, "Vui lòng nhập tên mẫu"),
    dosage: z.string().optional(),
    frequency: z.string().optional(),
    durationDays: z.coerce.number().min(0, "Số ngày không hợp lệ").optional(),
    instruction: z.string().min(1, "Vui lòng nhập hướng dẫn"),
    isDefault: z.boolean().default(false),
    sortOrder: z.coerce.number().default(0),
    isActive: z.boolean().default(true),
});

type TemplateFV = z.infer<typeof templateSchema>;

interface Props {
    medicine: Medicine | null;
    onClose: () => void;
}

export function MedicineUsageTemplateModal({ medicine, onClose }: Props) {
    const queryClient = useQueryClient();
    const [mode, setMode] = useState<"list" | "create" | "edit">("list");
    const [editingTemplate, setEditingTemplate] = useState<MedicineUsageTemplate | null>(null);

    const templatesQuery = useQuery({
        queryKey: ["admin", "medicines", medicine?.id, "usage-templates"],
        queryFn: () => getMedicineUsageTemplates(medicine!.id),
        enabled: !!medicine,
    });

    const form = useForm<TemplateFV>({
        resolver: zodResolver(templateSchema) as never,
        defaultValues: { label: "", dosage: "", frequency: "", durationDays: 0, instruction: "", isDefault: false, sortOrder: 0, isActive: true },
    });

    const refreshTemplates = () => queryClient.invalidateQueries({ queryKey: ["admin", "medicines", medicine?.id, "usage-templates"] });

    const createMutation = useMutation({
        mutationFn: (data: TemplateFV) => createMedicineUsageTemplate(medicine!.id, data),
        onSuccess: async () => {
            toast.success("Thêm mẫu thành công");
            setMode("list");
            await refreshTemplates();
        },
        onError: (error) => toast.error(error instanceof Error ? error.message : "Không thể thêm mẫu"),
    });

    const updateMutation = useMutation({
        mutationFn: ({ templateId, data }: { templateId: string; data: TemplateFV }) => updateMedicineUsageTemplate(medicine!.id, templateId, data),
        onSuccess: async () => {
            toast.success("Cập nhật mẫu thành công");
            setMode("list");
            await refreshTemplates();
        },
        onError: (error) => toast.error(error instanceof Error ? error.message : "Không thể cập nhật mẫu"),
    });

    const deleteMutation = useMutation({
        mutationFn: (templateId: string) => deleteMedicineUsageTemplate(medicine!.id, templateId),
        onSuccess: async () => {
            toast.success("Đã ngừng áp dụng mẫu");
            await refreshTemplates();
        },
        onError: () => toast.error("Không thể ngừng áp dụng mẫu"),
    });

    if (!medicine) return null;

    const submit = (data: TemplateFV) => {
        if (mode === "edit" && editingTemplate) {
            updateMutation.mutate({ templateId: editingTemplate.id, data });
            return;
        }
        createMutation.mutate(data);
    };

    const openCreate = () => {
        form.reset({ label: "", dosage: "", frequency: "", durationDays: 0, instruction: "", isDefault: false, sortOrder: 0, isActive: true });
        setEditingTemplate(null);
        setMode("create");
    };

    const openEdit = (template: MedicineUsageTemplate) => {
        form.reset({
            label: template.label,
            dosage: template.dosage || "",
            frequency: template.frequency || "",
            durationDays: template.durationDays || 0,
            instruction: template.instruction,
            isDefault: template.isDefault,
            sortOrder: template.sortOrder,
            isActive: template.isActive,
        });
        setEditingTemplate(template);
        setMode("edit");
    };

    const isSubmitting = createMutation.isPending || updateMutation.isPending;

    if (mode === "create" || mode === "edit") {
        return (
            <Modal isOpen={true} onClose={() => setMode("list")} title={mode === "edit" ? "Sửa Mẫu Liều" : "Thêm Mẫu Liều Mới"}>
                <form onSubmit={form.handleSubmit(submit)} className="space-y-4 pt-2">
                    <Input label="Tên mẫu" placeholder="VD: Liều trẻ em" {...form.register("label")} error={form.formState.errors.label?.message} />
                    <div className="grid grid-cols-2 gap-4">
                        <Input label="Liều dùng (Tùy chọn)" placeholder="VD: 1 viên" {...form.register("dosage")} error={form.formState.errors.dosage?.message} />
                        <Input label="Tần suất (Tùy chọn)" placeholder="VD: 2 lần/ngày" {...form.register("frequency")} error={form.formState.errors.frequency?.message} />
                    </div>
                    <div className="grid grid-cols-2 gap-4">
                        <Input type="number" label="Số ngày dùng (Tùy chọn)" placeholder="VD: 7" {...form.register("durationDays")} error={form.formState.errors.durationDays?.message} />
                        <Input type="number" label="Thứ tự hiển thị" {...form.register("sortOrder")} error={form.formState.errors.sortOrder?.message} />
                    </div>
                    <Input label="Hướng dẫn sử dụng chi tiết" placeholder="VD: 1 viên x 2 lần/ngày sau ăn" {...form.register("instruction")} error={form.formState.errors.instruction?.message} />
                    <div className="flex items-center gap-4">
                        <label className="flex items-center gap-2 text-sm text-slate-700">
                            <input type="checkbox" className="rounded" {...form.register("isDefault")} />
                            Mặc định
                        </label>
                        {mode === "edit" && (
                            <label className="flex items-center gap-2 text-sm text-slate-700">
                                <input type="checkbox" className="rounded" {...form.register("isActive")} />
                                Đang áp dụng
                            </label>
                        )}
                    </div>
                    <div className="flex justify-end gap-3 pt-2">
                        <Button variant="ghost" type="button" onClick={() => setMode("list")} disabled={isSubmitting}>Hủy</Button>
                        <Button type="submit" disabled={isSubmitting}>{isSubmitting ? "Đang lưu..." : "Lưu"}</Button>
                    </div>
                </form>
            </Modal>
        );
    }

    return (
        <Modal isOpen={true} onClose={onClose} title={`Mẫu Liều - ${medicine.name}`}>
            <div className="space-y-4 pt-2">
                <div className="flex justify-between items-center">
                    <p className="text-sm text-slate-500">Quản lý các mẫu liều dùng cho thuốc này để kê đơn nhanh hơn.</p>
                    <Button onClick={openCreate}>Thêm mẫu</Button>
                </div>
                {templatesQuery.isLoading ? (
                    <div className="flex items-center gap-2 p-6 text-sm text-slate-500 justify-center">
                        <span className="h-4 w-4 animate-spin rounded-full border-2 border-slate-300 border-t-primary-600" />
                        Đang tải mẫu liều...
                    </div>
                ) : (templatesQuery.data ?? []).length === 0 ? (
                    <EmptyState title="Trống" description="Chưa có mẫu liều nào" />
                ) : (
                    <DataTable
                        columns={["Tên mẫu", "Liều x Lần", "Số ngày", "Hướng dẫn", "Trạng thái", "Hành động"]}
                        rows={(templatesQuery.data ?? []).map((template) => [
                            <span key={`label-${template.id}`} className={template.isDefault ? "font-bold" : ""}>
                                {template.label} {template.isDefault && <Tag tone="blue">Mặc định</Tag>}
                            </span>,
                            <span key={`freq-${template.id}`}>
                                {template.dosage && template.frequency ? `${template.dosage} x ${template.frequency}` : "-"}
                            </span>,
                            template.durationDays || "-",
                            template.instruction,
                            template.isActive ? <Tag tone="green">Đang áp dụng</Tag> : <Tag tone="amber">Ngừng áp dụng</Tag>,
                            <div key={template.id} className="flex gap-1">
                                <Button variant="outline" className="h-auto px-2 py-1 text-xs" onClick={() => openEdit(template)}>Sửa</Button>
                                {template.isActive && (
                                    <Button
                                        variant="outline"
                                        className="h-auto border-red-200 px-2 py-1 text-xs text-red-600 hover:bg-red-50"
                                        onClick={() => {
                                            if (window.confirm(`Ngừng áp dụng mẫu "${template.label}"?`)) {
                                                deleteMutation.mutate(template.id);
                                            }
                                        }}
                                    >
                                        Xóa
                                    </Button>
                                )}
                            </div>,
                        ])}
                    />
                )}
            </div>
        </Modal>
    );
}
