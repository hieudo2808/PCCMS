import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Plus } from "lucide-react";
import { Button, Tag } from "~/components/atoms";
import { Card, EmptyState, Modal } from "~/components/molecules";
import { petApi } from "~/shared/api/petApi";
import { PetForm } from "../components/PetForm";

export function PetProfilesPage() {
    const [isAddModalOpen, setIsAddModalOpen] = useState(false);

    const {
        data: petsPage,
        isLoading,
        isError,
    } = useQuery({
        queryKey: ["pets"],
        queryFn: () => petApi.getPets(),
    });

    return (
        <div className="space-y-6">
            <div className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
                <div>
                    <h2 className="text-xl font-semibold tracking-tight">Hồ sơ thú cưng</h2>
                    <p className="text-sm text-slate-500">
                        Quản lý thông tin và theo dõi sức khỏe thú cưng của bạn
                    </p>
                </div>
                <div className="flex gap-2">
                    <Button variant="outline">Bộ lọc</Button>
                    <Button onClick={() => setIsAddModalOpen(true)}>
                        <span className="inline-flex items-center gap-2">
                            <Plus className="h-4 w-4" /> Thêm thú cưng
                        </span>
                    </Button>
                </div>
            </div>

            {isLoading && (
                <div className="flex justify-center p-8">
                    <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-indigo-600"></div>
                </div>
            )}

            {isError && (
                <EmptyState
                    title="Không thể tải danh sách thú cưng"
                    description="Vui lòng thử lại sau."
                />
            )}

            {!isLoading && !isError && petsPage?.content.length === 0 && (
                <EmptyState
                    title="Chưa có thú cưng nào"
                    description="Thêm thú cưng để bắt đầu theo dõi sức khỏe và lịch hẹn."
                />
            )}

            {!isLoading && !isError && petsPage?.content && petsPage.content.length > 0 && (
                <div className="grid gap-4 lg:grid-cols-3">
                    {petsPage.content.map((pet) => (
                        <Card key={pet.id} className="overflow-hidden p-0 relative">
                            {pet.healthAlerts && pet.healthAlerts.length > 0 && (
                                <div className="absolute top-2 right-2 z-10 flex flex-col gap-1">
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
                                            {pet.speciesId} • {pet.breedId}
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
                                        <p className="mt-1 font-medium">
                                            {pet.sex === "MALE"
                                                ? "Đực"
                                                : pet.sex === "FEMALE"
                                                  ? "Cái"
                                                  : "Chưa rõ"}
                                        </p>
                                    </div>
                                </div>
                                <div className="mt-4 flex gap-2">
                                    <Button variant="outline" className="flex-1">
                                        Xem chi tiết
                                    </Button>
                                    <Button variant="ghost" className="flex-1">
                                        Chỉnh sửa
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
        </div>
    );
}
