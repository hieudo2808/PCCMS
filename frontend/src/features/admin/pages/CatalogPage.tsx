import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { toast } from "react-hot-toast";
import { Button, Input } from "~/components/atoms";
import { Card, DataTable, EmptyState, Modal } from "~/components/molecules";
import { medicineApi } from "../api/medicineApi";
import type { MedicineResponse } from "~/types/medicine";
import { GroomingAdminPanel } from "../components/GroomingAdminPanel";

const medicineSchema = z.object({
    name: z.string().min(1, "Vui lòng nhập tên thuốc"),
    categoryId: z.string().min(1, "Vui lòng nhập danh mục"),
    unit: z.string().min(1, "Vui lòng nhập đơn vị"),
    unitPriceVnd: z.coerce.number().min(0, "Giá phải >= 0"),
    initialStock: z.coerce.number().min(0, "Tồn kho phải >= 0"),
    defaultInstruction: z.string().optional(),
});

type MedicineFormValues = z.infer<typeof medicineSchema>;

export function CatalogPage() {
    const queryClient = useQueryClient();
    const [page] = useState(1);
    const [isModalOpen, setIsModalOpen] = useState(false);

    const {
        data: pageResponse,
        isLoading,
        isError,
    } = useQuery({
        queryKey: ["medicines", page],
        queryFn: () => medicineApi.getMedicines(page, 10),
    });

    const createMutation = useMutation({
        mutationFn: (data: MedicineFormValues) =>
            medicineApi.createMedicine({
                ...data,
                defaultInstruction: data.defaultInstruction || "",
            }),
        onSuccess: () => {
            toast.success("Thêm thuốc thành công");
            setIsModalOpen(false);
            queryClient.invalidateQueries({ queryKey: ["medicines"] });
            reset();
        },
        onError: () => toast.error("Có lỗi xảy ra"),
    });

    const {
        register,
        handleSubmit,
        reset,
        formState: { errors },
    } = useForm<MedicineFormValues>({
        resolver: zodResolver(medicineSchema) as any,
        defaultValues: {
            name: "",
            categoryId: "",
            unit: "",
            unitPriceVnd: 0,
            initialStock: 0,
            defaultInstruction: "",
        },
    });

    if (isLoading) {
        return (
            <div className="flex justify-center p-8">
                <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-indigo-600"></div>
            </div>
        );
    }

    if (isError) {
        return <EmptyState title="Lỗi" description="Không thể tải danh sách thuốc" />;
    }

    const rows =
        pageResponse?.content.map((med: MedicineResponse) => [
            med.name,
            med.currentStock.toString(),
            med.unit,
            med.defaultInstruction || "-",
            <Button key={`edit-${med.id}`} variant="outline" className="px-2 py-1 h-auto text-xs">
                Sửa
            </Button>,
        ]) || [];

    return (
        <div className="grid gap-6">
            <Card
                title="Danh mục thuốc"
                right={<Button onClick={() => setIsModalOpen(true)}>Thêm thuốc mới</Button>}
            >
                <DataTable
                    columns={["Tên thuốc", "Tồn kho", "Đơn vị", "Hướng dẫn", "Hành động"]}
                    rows={rows}
                />
            </Card>

            <Modal
                isOpen={isModalOpen}
                onClose={() => setIsModalOpen(false)}
                title="Thêm Thuốc Mới"
            >
                <form
                    onSubmit={handleSubmit((data) => createMutation.mutate(data))}
                    className="space-y-4"
                >
                    <Input label="Tên thuốc" {...register("name")} error={errors.name?.message} />
                    <Input
                        label="Danh mục (ID)"
                        {...register("categoryId")}
                        error={errors.categoryId?.message}
                    />
                    <div className="grid grid-cols-2 gap-4">
                        <Input label="Đơn vị" {...register("unit")} error={errors.unit?.message} />
                        <Input
                            label="Giá (VND)"
                            type="number"
                            {...register("unitPriceVnd")}
                            error={errors.unitPriceVnd?.message}
                        />
                    </div>
                    <Input
                        label="Tồn kho ban đầu"
                        type="number"
                        {...register("initialStock")}
                        error={errors.initialStock?.message}
                    />
                    <Input label="Hướng dẫn mặc định" {...register("defaultInstruction")} />

                    <div className="mt-6 flex justify-end gap-2">
                        <Button
                            type="button"
                            variant="outline"
                            onClick={() => setIsModalOpen(false)}
                        >
                            Hủy
                        </Button>
                        <Button type="submit" disabled={createMutation.isPending}>
                            {createMutation.isPending ? "Đang lưu..." : "Lưu"}
                        </Button>
                    </div>
                </form>
            </Modal>

            <GroomingAdminPanel />
        </div>
    );
}
