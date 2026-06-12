import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Plus, Trash2 } from "lucide-react";
import toast from "react-hot-toast";
import { Button, Tag } from "~/components/atoms";
import { Card, EmptyState, Modal, SummaryRow } from "~/components/molecules";
import { petApi } from "~/shared/api/petApi";
import { PetForm } from "../components/PetForm";
import type { PetResponse } from "~/types/pet";

function sexLabel(sex: PetResponse["sex"]) {
    switch (sex) {
        case "MALE":
            return "Đực";
        case "FEMALE":
            return "Cái";
        default:
            return "Chưa rõ";
    }
}

export function PetProfilesPage() {
    const queryClient = useQueryClient();
    const navigate = useNavigate();
    const [isAddModalOpen, setIsAddModalOpen] = useState(false);
    const [viewingPet, setViewingPet] = useState<PetResponse | null>(null);
    const [editingPet, setEditingPet] = useState<PetResponse | null>(null);

    const {
        data: petsPage,
        isLoading,
        isError,
    } = useQuery({
        queryKey: ["pets"],
        queryFn: () => petApi.getPets(),
    });

    const deletePetMutation = useMutation({
        mutationFn: (petId: string) => petApi.deletePet(petId),
        onSuccess: () => {
            toast.success("Đã ẩn hồ sơ thú cưng");
            queryClient.invalidateQueries({ queryKey: ["pets"] });
        },
        onError: () => toast.error("Không thể ẩn hồ sơ thú cưng"),
    });

    const pets = petsPage?.content ?? [];

    return (
        <div className="space-y-6">
            <div className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
                <div>
                    <h2 className="text-xl font-semibold tracking-tight">Hồ sơ thú cưng</h2>
                    <p className="text-sm text-slate-500">
                        Quản lý thông tin nền, ghi chú chăm sóc và cảnh báo sức khỏe của thú cưng.
                    </p>
                </div>
                <Button onClick={() => setIsAddModalOpen(true)}>
                    <span className="inline-flex items-center gap-2">
                        <Plus className="h-4 w-4" /> Thêm thú cưng
                    </span>
                </Button>
            </div>

            {isLoading && (
                <div className="flex justify-center p-8">
                    <div className="h-8 w-8 animate-spin rounded-full border-b-2 border-indigo-600" />
                </div>
            )}

            {isError && (
                <EmptyState
                    title="Không thể tải danh sách thú cưng"
                    description="Vui lòng thử lại sau."
                />
            )}

            {!isLoading && !isError && pets.length === 0 && (
                <EmptyState
                    title="Chưa có thú cưng nào"
                    description="Thêm thú cưng để bắt đầu theo dõi sức khỏe và lịch hẹn."
                />
            )}

            {!isLoading && !isError && pets.length > 0 && (
                <div className="grid gap-4 lg:grid-cols-3">
                    {pets.map((pet) => (
                        <Card key={pet.id} className="relative overflow-hidden p-0">
                            {pet.healthAlerts?.length > 0 && (
                                <div className="absolute right-2 top-2 z-10 flex flex-col gap-1">
                                    {pet.healthAlerts.map((alert) => (
                                        <Tag
                                            key={alert.id}
                                            tone={
                                                alert.severity === "CRITICAL"
                                                    ? "red"
                                                    : alert.severity === "WARNING"
                                                      ? "amber"
                                                      : "blue"
                                            }
                                        >
                                            Cảnh báo: {alert.message}
                                        </Tag>
                                    ))}
                                </div>
                            )}
                            <div className="h-36 bg-linear-to-br from-amber-100 via-emerald-50 to-sky-100" />
                            <div className="p-5">
                                <div className="flex items-start justify-between gap-3">
                                    <div>
                                        <h3 className="text-lg font-semibold">{pet.name}</h3>
                                        <p className="text-sm text-slate-500">
                                            {pet.speciesName || "-"} • {pet.breedName || "-"}
                                        </p>
                                    </div>
                                    <Tag tone={pet.isActive ? "green" : "default"}>
                                        {pet.isActive ? "Đang hoạt động" : "Đã ẩn"}
                                    </Tag>
                                </div>
                                <div className="mt-4 grid grid-cols-2 gap-3 text-sm">
                                    <div className="rounded-2xl bg-slate-50 p-3">
                                        <p className="text-slate-500">Cân nặng</p>
                                        <p className="mt-1 font-medium">
                                            {pet.weightKg ? `${pet.weightKg} kg` : "Chưa cập nhật"}
                                        </p>
                                    </div>
                                    <div className="rounded-2xl bg-slate-50 p-3">
                                        <p className="text-slate-500">Giới tính</p>
                                        <p className="mt-1 font-medium">{sexLabel(pet.sex)}</p>
                                    </div>
                                </div>
                                <div className="mt-4 flex flex-wrap gap-2">
                                    <Button
                                        variant="outline"
                                        className="flex-1 px-3 py-1.5 text-xs"
                                        onClick={() => setViewingPet(pet)}
                                    >
                                        Chi tiết
                                    </Button>
                                    <Button
                                        variant="outline"
                                        className="flex-1 px-3 py-1.5 text-xs"
                                        onClick={() => navigate(`/owner/pets/${pet.id}/medical-records`)}
                                    >
                                        Lịch sử khám
                                    </Button>
                                    <Button
                                        variant="ghost"
                                        className="flex-1 px-3 py-1.5 text-xs"
                                        onClick={() => setEditingPet(pet)}
                                    >
                                        Sửa
                                    </Button>
                                    <Button
                                        variant="ghost"
                                        className="px-3 text-red-600"
                                        aria-label={`Ẩn hồ sơ ${pet.name}`}
                                        disabled={deletePetMutation.isPending}
                                        onClick={() => {
                                            if (window.confirm(`Ẩn hồ sơ thú cưng ${pet.name}?`)) {
                                                deletePetMutation.mutate(pet.id);
                                            }
                                        }}
                                    >
                                        <Trash2 className="h-4 w-4" />
                                    </Button>
                                </div>
                            </div>
                        </Card>
                    ))}
                </div>
            )}

            <Modal
                isOpen={isAddModalOpen}
                onClose={() => setIsAddModalOpen(false)}
                title="Thêm thú cưng mới"
            >
                <PetForm
                    onSuccess={() => setIsAddModalOpen(false)}
                    onCancel={() => setIsAddModalOpen(false)}
                />
            </Modal>

            <Modal
                isOpen={Boolean(editingPet)}
                onClose={() => setEditingPet(null)}
                title="Chỉnh sửa hồ sơ thú cưng"
            >
                {editingPet && (
                    <PetForm
                        pet={editingPet}
                        onSuccess={() => setEditingPet(null)}
                        onCancel={() => setEditingPet(null)}
                    />
                )}
            </Modal>

            <Modal
                isOpen={Boolean(viewingPet)}
                onClose={() => setViewingPet(null)}
                title="Chi tiết hồ sơ thú cưng"
            >
                {viewingPet && (
                    <div className="grid gap-4 md:grid-cols-2">
                        <SummaryRow label="Tên" value={viewingPet.name} />
                        <SummaryRow label="Loài" value={viewingPet.speciesName || "Chưa cập nhật"} />
                        <SummaryRow label="Giống" value={viewingPet.breedName || "Chưa cập nhật"} />
                        <SummaryRow label="Giới tính" value={sexLabel(viewingPet.sex)} />
                        <SummaryRow label="Ngày sinh" value={viewingPet.birthDate || "Chưa cập nhật"} />
                        <SummaryRow
                            label="Cân nặng"
                            value={viewingPet.weightKg ? `${viewingPet.weightKg} kg` : "Chưa cập nhật"}
                        />
                        <SummaryRow label="Màu lông" value={viewingPet.color || "Chưa cập nhật"} />
                        <SummaryRow
                            label="Trạng thái"
                            value={viewingPet.isActive ? "Đang hoạt động" : "Đã ẩn"}
                        />
                        <SummaryRow
                            label="Dấu hiệu nhận diện"
                            value={viewingPet.identificationNote || "Không có"}
                        />
                        <SummaryRow label="Dị ứng" value={viewingPet.allergyNote || "Không có"} />
                        <SummaryRow label="Dinh dưỡng" value={viewingPet.nutritionNote || "Không có"} />
                        <SummaryRow
                            label="Ghi chú đặc biệt"
                            value={viewingPet.specialNote || "Không có"}
                        />
                    </div>
                )}
            </Modal>
        </div>
    );
}
