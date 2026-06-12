import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { toast } from "react-hot-toast";
import { z } from "zod";
import { Button } from "~/components/atoms/Button";
import { Input } from "~/components/atoms/Input";
import { Tag } from "~/components/atoms/Tag";
import { Card } from "~/components/molecules/Card";
import { DataTable } from "~/components/molecules/DataTable";
import { EmptyState } from "~/components/molecules/EmptyState";
import { Modal } from "~/components/molecules/Modal";
import { catalogApi } from "~/features/admin/api/catalogApi";
import { createMedicine, deleteMedicine, getMedicines, updateMedicine } from "~/features/admin/medicine-management/medicineService";
import type { Medicine, MedicineFormValues } from "~/features/admin/medicine-management/types";
import { MedicineUsageTemplateModal } from "./MedicineUsageTemplateModal";
import { createService, deleteService, getServices, updateService } from "~/features/admin/service-category-management/serviceCategoryService";
import type { Service, ServiceFormValues } from "~/features/admin/service-category-management/types";
import type { MedicineCategoryResponse } from "~/types/catalog";

const medicineSchema = z.object({
    code: z.string().optional().default(""),
    name: z.string().min(1, "Vui lòng nhập tên thuốc"),
    categoryId: z.string().min(1, "Vui lòng chọn nhóm thuốc"),
    group: z.string().optional().default(""),
    unit: z.string().min(1, "Vui lòng nhập đơn vị"),
    stock: z.string().min(1, "Vui lòng nhập tồn kho ban đầu"),
    unitPriceVnd: z.string().min(1, "Vui lòng nhập giá"),
    note: z.string().optional().default(""),
});

const serviceSchema = z.object({
    code: z.string().min(1, "Vui lòng nhập mã dịch vụ"),
    name: z.string().min(1, "Vui lòng nhập tên dịch vụ"),
    type: z.enum(["Khám bệnh", "Làm đẹp", "Lưu trú", "Khác", ""]),
    price: z.string().min(1, "Vui lòng nhập đơn giá"),
    durationMinutes: z.string().optional().default(""),
    description: z.string().optional().default(""),
    status: z.enum(["active", "inactive", ""]),
});

const medCategorySchema = z.object({
    name: z.string().min(1, "Vui lòng nhập tên nhóm"),
    description: z.string().optional().default(""),
    isActive: z.boolean().default(true),
});

type MedicineFV = z.infer<typeof medicineSchema>;
type ServiceFV = z.infer<typeof serviceSchema>;
type MedCategoryFV = z.infer<typeof medCategorySchema>;
type Tab = "medicines" | "services" | "medCategories";

const medicineDefaults: MedicineFV = {
    code: "",
    name: "",
    categoryId: "",
    group: "",
    unit: "",
    stock: "",
    unitPriceVnd: "",
    note: "",
};

export function CatalogPage() {
    const queryClient = useQueryClient();
    const [tab, setTab] = useState<Tab>("medicines");
    const [medicineModal, setMedicineModal] = useState<"create" | "edit" | null>(null);
    const [editingMedicine, setEditingMedicine] = useState<Medicine | null>(null);
    const [serviceModal, setServiceModal] = useState<"create" | "edit" | null>(null);
    const [editingService, setEditingService] = useState<Service | null>(null);
    const [medCatModal, setMedCatModal] = useState<"create" | "edit" | null>(null);
    const [editingMedCat, setEditingMedCat] = useState<MedicineCategoryResponse | null>(null);
    const [templateMedicine, setTemplateMedicine] = useState<Medicine | null>(null);

    const medicinesQuery = useQuery({ queryKey: ["admin", "medicines"], queryFn: getMedicines });
    const servicesQuery = useQuery({ queryKey: ["admin", "services"], queryFn: getServices });
    const medCategoriesQuery = useQuery({
        queryKey: ["admin", "medicine-categories"],
        queryFn: async () => {
            const response = await catalogApi.listMedicineCategories(false) as any;
            return (response?.data ?? response) as MedicineCategoryResponse[];
        },
    });

    const medForm = useForm<MedicineFV>({
        resolver: zodResolver(medicineSchema) as never,
        defaultValues: medicineDefaults,
    });
    const svcForm = useForm<ServiceFV>({
        resolver: zodResolver(serviceSchema) as never,
        defaultValues: { code: "", name: "", type: "Làm đẹp", price: "", durationMinutes: "", description: "", status: "active" },
    });
    const medCatForm = useForm<MedCategoryFV>({
        resolver: zodResolver(medCategorySchema) as never,
        defaultValues: { name: "", description: "", isActive: true },
    });

    const refreshMedicines = () => queryClient.invalidateQueries({ queryKey: ["admin", "medicines"] });
    const refreshServices = () => queryClient.invalidateQueries({ queryKey: ["admin", "services"] });
    const refreshCategories = () => queryClient.invalidateQueries({ queryKey: ["admin", "medicine-categories"] });

    const createMedMutation = useMutation({
        mutationFn: (data: MedicineFormValues) => createMedicine(data),
        onSuccess: async () => {
            toast.success("Thêm thuốc thành công");
            closeMedicineModal();
            await refreshMedicines();
        },
        onError: (error) => toast.error(error instanceof Error ? error.message : "Không thể thêm thuốc"),
    });
    const updateMedMutation = useMutation({
        mutationFn: ({ id, data }: { id: string; data: MedicineFormValues }) => updateMedicine(id, data),
        onSuccess: async () => {
            toast.success("Cập nhật thuốc thành công");
            closeMedicineModal();
            await refreshMedicines();
        },
        onError: (error) => toast.error(error instanceof Error ? error.message : "Không thể cập nhật thuốc"),
    });
    const deleteMedMutation = useMutation({
        mutationFn: deleteMedicine,
        onSuccess: async () => {
            toast.success("Đã ngừng áp dụng thuốc");
            await refreshMedicines();
        },
        onError: () => toast.error("Không thể ngừng áp dụng thuốc"),
    });

    const createSvcMutation = useMutation({
        mutationFn: (data: ServiceFormValues) => createService(data),
        onSuccess: async () => {
            toast.success("Thêm dịch vụ thành công");
            closeServiceModal();
            await refreshServices();
        },
        onError: () => toast.error("Không thể thêm dịch vụ"),
    });
    const updateSvcMutation = useMutation({
        mutationFn: ({ id, data }: { id: string; data: ServiceFormValues }) => updateService(id, data),
        onSuccess: async () => {
            toast.success("Cập nhật dịch vụ thành công");
            closeServiceModal();
            await refreshServices();
        },
        onError: () => toast.error("Không thể cập nhật dịch vụ"),
    });
    const deleteSvcMutation = useMutation({
        mutationFn: deleteService,
        onSuccess: async () => {
            toast.success("Đã ngừng áp dụng dịch vụ");
            await refreshServices();
        },
        onError: () => toast.error("Không thể ngừng áp dụng dịch vụ"),
    });

    const createMedCatMutation = useMutation({
        mutationFn: (data: MedCategoryFV) => catalogApi.createMedicineCategory(data),
        onSuccess: async () => {
            toast.success("Thêm nhóm thuốc thành công");
            closeMedCatModal();
            await refreshCategories();
        },
        onError: () => toast.error("Không thể thêm nhóm thuốc"),
    });
    const updateMedCatMutation = useMutation({
        mutationFn: ({ id, data }: { id: string; data: MedCategoryFV }) => catalogApi.updateMedicineCategory(id, data),
        onSuccess: async () => {
            toast.success("Cập nhật nhóm thuốc thành công");
            closeMedCatModal();
            await refreshCategories();
        },
        onError: () => toast.error("Không thể cập nhật nhóm thuốc"),
    });

    function closeMedicineModal() {
        setMedicineModal(null);
        setEditingMedicine(null);
        medForm.reset(medicineDefaults);
    }

    function closeServiceModal() {
        setServiceModal(null);
        setEditingService(null);
        svcForm.reset();
    }

    function closeMedCatModal() {
        setMedCatModal(null);
        setEditingMedCat(null);
        medCatForm.reset({ name: "", description: "", isActive: true });
    }

    function openCreateMedicine() {
        medForm.reset(medicineDefaults);
        setEditingMedicine(null);
        setMedicineModal("create");
    }

    function openEditMedicine(medicine: Medicine) {
        medForm.reset({
            code: medicine.code,
            name: medicine.name,
            categoryId: medicine.categoryId || "",
            group: medicine.group,
            unit: medicine.unit,
            stock: String(medicine.stock),
            unitPriceVnd: String(medicine.unitPriceVnd ?? 0),
            note: medicine.note,
        });
        setEditingMedicine(medicine);
        setMedicineModal("edit");
    }

    function submitMedicine(data: MedicineFV) {
        if (medicineModal === "edit" && editingMedicine) {
            updateMedMutation.mutate({ id: editingMedicine.id, data });
            return;
        }
        createMedMutation.mutate(data);
    }

    function openCreateService() {
        svcForm.reset({ code: "", name: "", type: "Làm đẹp", price: "", durationMinutes: "", description: "", status: "active" });
        setEditingService(null);
        setServiceModal("create");
    }

    function openEditService(service: Service) {
        svcForm.reset({
            code: service.code,
            name: service.name,
            type: service.type,
            price: String(service.price),
            durationMinutes: service.durationMinutes ? String(service.durationMinutes) : "",
            description: service.description,
            status: service.status,
        });
        setEditingService(service);
        setServiceModal("edit");
    }

    function submitService(data: ServiceFV) {
        if (serviceModal === "edit" && editingService) {
            updateSvcMutation.mutate({ id: editingService.id, data });
            return;
        }
        createSvcMutation.mutate(data);
    }

    function openCreateMedCat() {
        medCatForm.reset({ name: "", description: "", isActive: true });
        setEditingMedCat(null);
        setMedCatModal("create");
    }

    function openEditMedCat(category: MedicineCategoryResponse) {
        medCatForm.reset({ name: category.name, description: category.description || "", isActive: category.isActive });
        setEditingMedCat(category);
        setMedCatModal("edit");
    }

    function submitMedCat(data: MedCategoryFV) {
        if (medCatModal === "edit" && editingMedCat) {
            updateMedCatMutation.mutate({ id: editingMedCat.id, data });
            return;
        }
        createMedCatMutation.mutate(data);
    }

    const medCategories = medCategoriesQuery.data ?? [];
    const activeCategories = medCategories.filter((category) => category.isActive);
    const isMedSubmitting = createMedMutation.isPending || updateMedMutation.isPending;
    const isSvcSubmitting = createSvcMutation.isPending || updateSvcMutation.isPending;
    const isMedCatSubmitting = createMedCatMutation.isPending || updateMedCatMutation.isPending;

    return (
        <div className="grid gap-6">
            <div className="flex flex-wrap gap-2">
                <Button variant={tab === "medicines" ? "primary" : "outline"} onClick={() => setTab("medicines")}>Thuốc</Button>
                <Button variant={tab === "services" ? "primary" : "outline"} onClick={() => setTab("services")}>Dịch vụ</Button>
                <Button variant={tab === "medCategories" ? "primary" : "outline"} onClick={() => setTab("medCategories")}>Nhóm thuốc</Button>
            </div>

            {tab === "medicines" && (
                <Card title="Danh mục thuốc" right={<Button onClick={openCreateMedicine} disabled={activeCategories.length === 0}>Thêm thuốc mới</Button>}>
                    {activeCategories.length === 0 && (
                        <div className="mb-4 rounded-lg border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-800">
                            Cần tạo ít nhất một nhóm thuốc đang áp dụng trước khi thêm thuốc.
                        </div>
                    )}
                    {medicinesQuery.isLoading ? (
                        <div className="flex items-center gap-2 p-6 text-sm text-slate-500">
                            <span className="h-4 w-4 animate-spin rounded-full border-2 border-slate-300 border-t-primary-600" />
                            Đang tải danh sách thuốc...
                        </div>
                    ) : medicinesQuery.isError ? (
                        <EmptyState title="Lỗi" description="Không thể tải danh sách thuốc" />
                    ) : (medicinesQuery.data ?? []).length === 0 ? (
                        <EmptyState title="Trống" description="Chưa có thuốc nào" />
                    ) : (
                        <DataTable
                            columns={["Tên thuốc", "Nhóm", "Đơn vị", "Tồn kho", "Đơn giá", "Trạng thái", "Hành động"]}
                            rows={(medicinesQuery.data ?? []).map((medicine) => [
                                medicine.name,
                                medicine.group || "-",
                                medicine.unit,
                                medicine.stock,
                                medicine.unitPriceVnd ? `${medicine.unitPriceVnd.toLocaleString("vi-VN")}đ` : "-",
                                medicine.note ? <Tag tone="amber">Ngừng áp dụng</Tag> : <Tag tone="green">Đang áp dụng</Tag>,
                                <div key={medicine.id} className="flex gap-1">
                                    <Button variant="outline" className="h-auto px-2 py-1 text-xs" onClick={() => openEditMedicine(medicine)}>Sửa</Button>
                                    <Button variant="outline" className="h-auto px-2 py-1 text-xs" onClick={() => setTemplateMedicine(medicine)}>Mẫu liều</Button>
                                    <Button
                                        variant="outline"
                                        className="h-auto border-red-200 px-2 py-1 text-xs text-red-600 hover:bg-red-50"
                                        onClick={() => {
                                            if (window.confirm(`Ngừng áp dụng thuốc "${medicine.name}"?`)) {
                                                deleteMedMutation.mutate(medicine.id);
                                            }
                                        }}
                                    >
                                        Xóa
                                    </Button>
                                </div>,
                            ])}
                        />
                    )}
                </Card>
            )}

            {tab === "services" && (
                <Card title="Danh mục dịch vụ" right={<Button onClick={openCreateService}>Thêm dịch vụ mới</Button>}>
                    {servicesQuery.isLoading ? (
                        <div className="flex items-center gap-2 p-6 text-sm text-slate-500">
                            <span className="h-4 w-4 animate-spin rounded-full border-2 border-slate-300 border-t-primary-600" />
                            Đang tải danh sách dịch vụ...
                        </div>
                    ) : servicesQuery.isError ? (
                        <EmptyState title="Lỗi" description="Không thể tải danh sách dịch vụ" />
                    ) : (servicesQuery.data ?? []).length === 0 ? (
                        <EmptyState title="Trống" description="Chưa có dịch vụ nào" />
                    ) : (
                        <DataTable
                            columns={["Mã", "Tên dịch vụ", "Loại", "Đơn giá", "Thời gian", "Trạng thái", "Hành động"]}
                            rows={(servicesQuery.data ?? []).map((service) => [
                                service.code,
                                service.name,
                                service.type,
                                `${service.price.toLocaleString("vi-VN")}đ`,
                                service.durationMinutes ? `${service.durationMinutes} phút` : "-",
                                service.status === "active" ? <Tag tone="green">Đang hoạt động</Tag> : <Tag tone="amber">Ngừng hoạt động</Tag>,
                                <div key={service.id} className="flex gap-1">
                                    <Button variant="outline" className="h-auto px-2 py-1 text-xs" onClick={() => openEditService(service)}>Sửa</Button>
                                    {service.status === "active" && (
                                        <Button variant="outline" className="h-auto border-red-200 px-2 py-1 text-xs text-red-600 hover:bg-red-50" onClick={() => deleteSvcMutation.mutate(service.id)}>
                                            Xóa
                                        </Button>
                                    )}
                                </div>,
                            ])}
                        />
                    )}
                </Card>
            )}

            {tab === "medCategories" && (
                <Card title="Quản lý nhóm thuốc" right={<Button onClick={openCreateMedCat}>Thêm nhóm thuốc</Button>}>
                    {medCategoriesQuery.isLoading ? (
                        <div className="flex items-center gap-2 p-6 text-sm text-slate-500">
                            <span className="h-4 w-4 animate-spin rounded-full border-2 border-slate-300 border-t-primary-600" />
                            Đang tải nhóm thuốc...
                        </div>
                    ) : medCategories.length === 0 ? (
                        <EmptyState title="Trống" description="Chưa có nhóm thuốc nào" />
                    ) : (
                        <DataTable
                            columns={["Tên nhóm", "Mô tả", "Trạng thái", "Hành động"]}
                            rows={medCategories.map((category) => [
                                category.name,
                                category.description || "-",
                                category.isActive ? <Tag tone="green">Đang áp dụng</Tag> : <Tag tone="amber">Ngừng áp dụng</Tag>,
                                <Button key={category.id} variant="outline" className="h-auto px-2 py-1 text-xs" onClick={() => openEditMedCat(category)}>Sửa</Button>,
                            ])}
                        />
                    )}
                </Card>
            )}

            <Modal isOpen={medicineModal !== null} onClose={closeMedicineModal} title={medicineModal === "edit" ? "Sửa thuốc" : "Thêm Thuốc Mới"}>
                <form onSubmit={medForm.handleSubmit(submitMedicine)} className="space-y-4 pt-2">
                    <div className="grid gap-4 md:grid-cols-2">
                        <Input label="Tên thuốc" placeholder="Nhập tên thuốc" {...medForm.register("name")} error={medForm.formState.errors.name?.message} />
                        <div className="flex flex-col gap-1.5">
                            <label htmlFor="med-category" className="text-[13px] font-medium text-slate-700">Nhóm thuốc</label>
                            <select id="med-category" className="h-10 rounded-xl border border-slate-200 bg-white px-3 text-sm outline-none focus:border-primary-500 focus:ring-2 focus:ring-primary-500/20" {...medForm.register("categoryId")}>
                                <option value="">Chọn nhóm thuốc</option>
                                {activeCategories.map((category) => <option key={category.id} value={category.id}>{category.name}</option>)}
                            </select>
                            {medForm.formState.errors.categoryId && <p className="text-[12px] font-medium text-error-600">{medForm.formState.errors.categoryId.message}</p>}
                        </div>
                        <Input label="Đơn vị" placeholder="Viên, chai, hộp..." {...medForm.register("unit")} error={medForm.formState.errors.unit?.message} />
                        <Input label="Tồn kho ban đầu" type="number" min="0" {...medForm.register("stock")} error={medForm.formState.errors.stock?.message} />
                        <Input label="Giá (VND)" type="number" min="0" {...medForm.register("unitPriceVnd")} error={medForm.formState.errors.unitPriceVnd?.message} />
                    </div>
                    <div className="flex justify-end gap-3 pt-2">
                        <Button variant="ghost" type="button" onClick={closeMedicineModal} disabled={isMedSubmitting}>Hủy</Button>
                        <Button type="submit" disabled={isMedSubmitting}>{isMedSubmitting ? "Đang lưu..." : "Lưu"}</Button>
                    </div>
                </form>
            </Modal>

            <Modal isOpen={serviceModal !== null} onClose={closeServiceModal} title={serviceModal === "edit" ? "Sửa dịch vụ" : "Thêm Dịch Vụ Mới"}>
                <form onSubmit={svcForm.handleSubmit(submitService)} className="space-y-4 pt-2">
                    <div className="grid gap-4 md:grid-cols-2">
                        <Input label="Mã dịch vụ" {...svcForm.register("code")} error={svcForm.formState.errors.code?.message} />
                        <Input label="Tên dịch vụ" {...svcForm.register("name")} error={svcForm.formState.errors.name?.message} />
                        <div className="flex flex-col gap-1.5">
                            <label htmlFor="svc-type" className="text-[13px] font-medium text-slate-700">Loại dịch vụ</label>
                            <select id="svc-type" className="h-10 rounded-xl border border-slate-200 bg-white px-3 text-sm outline-none focus:border-primary-500 focus:ring-2 focus:ring-primary-500/20" {...svcForm.register("type")}>
                                <option value="Khám bệnh">Khám bệnh</option>
                                <option value="Làm đẹp">Làm đẹp</option>
                                <option value="Lưu trú">Lưu trú</option>
                                <option value="Khác">Khác</option>
                            </select>
                        </div>
                        <Input label="Đơn giá (VND)" type="number" min="0" {...svcForm.register("price")} error={svcForm.formState.errors.price?.message} />
                        <Input label="Thời gian (phút)" type="number" min="0" {...svcForm.register("durationMinutes")} />
                        <div className="flex flex-col gap-1.5">
                            <label htmlFor="svc-status" className="text-[13px] font-medium text-slate-700">Trạng thái</label>
                            <select id="svc-status" className="h-10 rounded-xl border border-slate-200 bg-white px-3 text-sm outline-none focus:border-primary-500 focus:ring-2 focus:ring-primary-500/20" {...svcForm.register("status")}>
                                <option value="active">Đang hoạt động</option>
                                <option value="inactive">Ngừng hoạt động</option>
                            </select>
                        </div>
                    </div>
                    <Input label="Mô tả" {...svcForm.register("description")} />
                    <div className="flex justify-end gap-3 pt-2">
                        <Button variant="ghost" type="button" onClick={closeServiceModal} disabled={isSvcSubmitting}>Hủy</Button>
                        <Button type="submit" disabled={isSvcSubmitting}>{isSvcSubmitting ? "Đang lưu..." : "Lưu"}</Button>
                    </div>
                </form>
            </Modal>

            <Modal isOpen={medCatModal !== null} onClose={closeMedCatModal} title={medCatModal === "edit" ? "Sửa nhóm thuốc" : "Thêm nhóm thuốc mới"}>
                <form onSubmit={medCatForm.handleSubmit(submitMedCat)} className="space-y-4 pt-2">
                    <Input label="Tên nhóm thuốc" {...medCatForm.register("name")} error={medCatForm.formState.errors.name?.message} />
                    <Input label="Mô tả" {...medCatForm.register("description")} />
                    <label className="flex items-center gap-2 text-sm text-slate-700">
                        <input type="checkbox" className="rounded" {...medCatForm.register("isActive")} />
                        Đang áp dụng
                    </label>
                    <div className="flex justify-end gap-3 pt-2">
                        <Button variant="ghost" type="button" onClick={closeMedCatModal} disabled={isMedCatSubmitting}>Hủy</Button>
                        <Button type="submit" disabled={isMedCatSubmitting}>{isMedCatSubmitting ? "Đang lưu..." : "Lưu"}</Button>
                    </div>
                </form>
            </Modal>

            <MedicineUsageTemplateModal medicine={templateMedicine} onClose={() => setTemplateMedicine(null)} />
        </div>
    );
}
