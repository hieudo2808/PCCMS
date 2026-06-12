import { useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { toast } from "react-hot-toast";
import { z } from "zod";
import { Button } from "~/components/atoms/Button";
import { Input } from "~/components/atoms/Input";
import { Tag } from "~/components/atoms/Tag";
import { Card } from "~/components/molecules/Card";
import { EmptyState } from "~/components/molecules/EmptyState";
import { Modal } from "~/components/molecules/Modal";
import { adminRoomApi } from "~/features/admin/api/adminRoomApi";
import type { RoomResponse, RoomStatus, RoomTypeResponse } from "~/types/catalog";

const roomStatuses: RoomStatus[] = ["AVAILABLE", "OCCUPIED", "MAINTENANCE", "INACTIVE"];

const statusLabel: Record<RoomStatus, string> = {
    AVAILABLE: "Trống",
    OCCUPIED: "Đang sử dụng",
    MAINTENANCE: "Bảo trì",
    INACTIVE: "Ngừng áp dụng",
};

const statusTone: Record<RoomStatus, "green" | "blue" | "amber" | "red"> = {
    AVAILABLE: "green",
    OCCUPIED: "blue",
    MAINTENANCE: "amber",
    INACTIVE: "red",
};

const roomSchema = z.object({
    roomCode: z.string().trim().max(60).optional().default(""),
    name: z.string().trim().min(1, "Vui lòng nhập tên phòng").max(120),
    roomTypeId: z.string().min(1, "Vui lòng chọn loại phòng"),
    floor: z.coerce.number().int().min(1, "Tầng phải lớn hơn 0"),
    capacity: z.coerce.number().int().min(1, "Sức chứa phải lớn hơn 0"),
    statusCode: z.enum(["AVAILABLE", "OCCUPIED", "MAINTENANCE", "INACTIVE"]),
    description: z.string().optional().default(""),
});

const roomTypeSchema = z.object({
    code: z.string().trim().max(60).optional().default(""),
    name: z.string().trim().min(1, "Vui lòng nhập tên loại").max(120),
    defaultCapacity: z.coerce.number().int().min(1, "Sức chứa phải lớn hơn 0"),
    baseDailyPriceVnd: z.coerce.number().int().min(0, "Giá không hợp lệ"),
    description: z.string().optional().default(""),
    isActive: z.preprocess((value) => value === true || value === "true", z.boolean()),
});

type RoomFormValues = z.infer<typeof roomSchema>;
type RoomTypeFormValues = z.infer<typeof roomTypeSchema>;
type RoomModalMode = "create" | "edit" | "view";
type RoomTypeModalMode = "create" | "edit" | "view";

const emptyRoom: RoomFormValues = {
    roomCode: "",
    name: "",
    roomTypeId: "",
    floor: 1,
    capacity: 1,
    statusCode: "AVAILABLE",
    description: "",
};

const emptyRoomType: RoomTypeFormValues = {
    code: "",
    name: "",
    defaultCapacity: 1,
    baseDailyPriceVnd: 0,
    description: "",
    isActive: true,
};

function formatCurrency(value?: number) {
    return new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND" }).format(value ?? 0);
}

function errorMessage(error: unknown, fallback: string) {
    return error instanceof Error && error.message ? error.message : fallback;
}

export function RoomsPage() {
    const queryClient = useQueryClient();
    const [tab, setTab] = useState<"rooms" | "roomTypes">("rooms");
    const [keyword, setKeyword] = useState("");
    const [roomTypeFilter, setRoomTypeFilter] = useState("");
    const [statusFilter, setStatusFilter] = useState<RoomStatus | "">("");
    const [roomModalMode, setRoomModalMode] = useState<RoomModalMode | null>(null);
    const [roomTypeModalMode, setRoomTypeModalMode] = useState<RoomTypeModalMode | null>(null);
    const [selectedRoom, setSelectedRoom] = useState<RoomResponse | null>(null);
    const [selectedRoomType, setSelectedRoomType] = useState<RoomTypeResponse | null>(null);
    const [deleteRoomTarget, setDeleteRoomTarget] = useState<RoomResponse | null>(null);
    const [deleteRoomTypeTarget, setDeleteRoomTypeTarget] = useState<RoomTypeResponse | null>(null);

    const roomForm = useForm<RoomFormValues>({ resolver: zodResolver(roomSchema) as never, defaultValues: emptyRoom });
    const roomTypeForm = useForm<RoomTypeFormValues>({ resolver: zodResolver(roomTypeSchema) as never, defaultValues: emptyRoomType });

    const roomTypesQuery = useQuery({ queryKey: ["admin", "room-types", "all"], queryFn: () => adminRoomApi.listRoomTypes(false) });
    const roomsQuery = useQuery({
        queryKey: ["admin", "rooms", roomTypeFilter, statusFilter],
        queryFn: () => adminRoomApi.listRooms({ page: 1, size: 100, roomTypeId: roomTypeFilter, statusCode: statusFilter }),
    });

    const roomTypes = roomTypesQuery.data ?? [];
    const rooms = roomsQuery.data?.content ?? [];
    const filteredRooms = useMemo(() => {
        const normalized = keyword.trim().toLowerCase();
        if (!normalized) return rooms;
        return rooms.filter((room) =>
            [room.roomCode, room.name, room.roomTypeName, room.description ?? ""].some((value) => value.toLowerCase().includes(normalized))
        );
    }, [keyword, rooms]);
    const activeRoomTypes = roomTypes.filter((roomType) => roomType.isActive || roomType.id === selectedRoom?.roomTypeId);

    const invalidateRooms = () => {
        queryClient.invalidateQueries({ queryKey: ["admin", "rooms"] });
        queryClient.invalidateQueries({ queryKey: ["admin", "room-types"] });
    };

    const saveRoomMutation = useMutation({
        mutationFn: (values: RoomFormValues) => {
            const payload = { ...values, description: values.description?.trim() || undefined };
            return selectedRoom && roomModalMode === "edit" ? adminRoomApi.updateRoom(selectedRoom.id, payload) : adminRoomApi.createRoom(payload);
        },
        onSuccess: () => {
            toast.success(roomModalMode === "edit" ? "Đã cập nhật phòng" : "Đã thêm phòng");
            closeRoomModal();
            invalidateRooms();
        },
        onError: (error) => toast.error(errorMessage(error, "Không thể lưu phòng")),
    });

    const saveRoomTypeMutation = useMutation({
        mutationFn: (values: RoomTypeFormValues) => {
            const payload = { ...values, code: values.code?.trim() || undefined, description: values.description?.trim() || undefined };
            return selectedRoomType && roomTypeModalMode === "edit"
                ? adminRoomApi.updateRoomType(selectedRoomType.id, payload)
                : adminRoomApi.createRoomType(payload);
        },
        onSuccess: () => {
            toast.success(roomTypeModalMode === "edit" ? "Đã cập nhật loại phòng" : "Đã thêm loại phòng");
            closeRoomTypeModal();
            invalidateRooms();
        },
        onError: (error) => toast.error(errorMessage(error, "Không thể lưu loại phòng")),
    });

    const deleteRoomMutation = useMutation({
        mutationFn: (room: RoomResponse) => adminRoomApi.deleteRoom(room.id),
        onSuccess: () => {
            toast.success("Đã ngừng áp dụng phòng");
            setDeleteRoomTarget(null);
            invalidateRooms();
        },
        onError: (error) => toast.error(errorMessage(error, "Không thể xóa phòng")),
    });

    const deleteRoomTypeMutation = useMutation({
        mutationFn: (roomType: RoomTypeResponse) => adminRoomApi.deleteRoomType(roomType.id),
        onSuccess: () => {
            toast.success("Đã ngừng áp dụng loại phòng");
            setDeleteRoomTypeTarget(null);
            invalidateRooms();
        },
        onError: (error) => toast.error(errorMessage(error, "Không thể xóa loại phòng đang được sử dụng")),
    });

    function closeRoomModal() {
        setRoomModalMode(null);
        setSelectedRoom(null);
        roomForm.reset(emptyRoom);
    }

    function closeRoomTypeModal() {
        setRoomTypeModalMode(null);
        setSelectedRoomType(null);
        roomTypeForm.reset(emptyRoomType);
    }

    function openCreateRoom() {
        setSelectedRoom(null);
        roomForm.reset(emptyRoom);
        setRoomModalMode("create");
    }

    function openRoom(room: RoomResponse, mode: RoomModalMode) {
        setSelectedRoom(room);
        roomForm.reset({
            roomCode: room.roomCode,
            name: room.name,
            roomTypeId: room.roomTypeId,
            floor: room.floor,
            capacity: room.capacity,
            statusCode: room.statusCode,
            description: room.description ?? "",
        });
        setRoomModalMode(mode);
    }

    function openCreateRoomType() {
        setSelectedRoomType(null);
        roomTypeForm.reset(emptyRoomType);
        setRoomTypeModalMode("create");
    }

    function openRoomType(roomType: RoomTypeResponse, mode: RoomTypeModalMode) {
        setSelectedRoomType(roomType);
        roomTypeForm.reset({
            code: roomType.code,
            name: roomType.name,
            defaultCapacity: roomType.defaultCapacity,
            baseDailyPriceVnd: roomType.baseDailyPriceVnd,
            description: roomType.description ?? "",
            isActive: roomType.isActive,
        });
        setRoomTypeModalMode(mode);
    }

    const roomReadOnly = roomModalMode === "view";
    const roomTypeReadOnly = roomTypeModalMode === "view";
    const roomModalTitle = roomReadOnly ? "Chi tiết phòng" : roomModalMode === "edit" ? "Sửa phòng" : "Thêm phòng";
    const roomTypeModalTitle = roomTypeReadOnly ? "Chi tiết loại phòng" : roomTypeModalMode === "edit" ? "Sửa loại phòng" : "Thêm loại phòng";

    return (
        <div className="space-y-5">
            <div className="flex flex-wrap items-center justify-between gap-3">
                <div>
                    <h1 className="text-2xl font-semibold text-slate-900">Quản lý phòng lưu trú</h1>
                    <p className="mt-1 text-sm text-slate-500">Quản lý phòng, loại phòng và trạng thái vận hành.</p>
                </div>
                <div className="flex gap-2">
                    <Button variant={tab === "rooms" ? "primary" : "outline"} onClick={() => setTab("rooms")}>Phòng</Button>
                    <Button variant={tab === "roomTypes" ? "primary" : "outline"} onClick={() => setTab("roomTypes")}>Loại phòng</Button>
                </div>
            </div>

            {tab === "rooms" ? (
                <Card title="Danh sách phòng" right={<Button onClick={openCreateRoom}>Thêm phòng</Button>}>
                    <div className="mb-4 grid gap-3 md:grid-cols-[1fr_220px_180px]">
                        <Input aria-label="Tìm phòng" placeholder="Tìm theo mã, tên, loại phòng" value={keyword} onChange={(event) => setKeyword(event.target.value)} />
                        <select className="h-10 rounded-xl border border-slate-200 bg-white px-3 text-sm text-slate-900" value={roomTypeFilter} onChange={(event) => setRoomTypeFilter(event.target.value)} aria-label="Lọc loại phòng">
                            <option value="">Tất cả loại phòng</option>
                            {roomTypes.map((roomType) => <option key={roomType.id} value={roomType.id}>{roomType.name}</option>)}
                        </select>
                        <select className="h-10 rounded-xl border border-slate-200 bg-white px-3 text-sm text-slate-900" value={statusFilter} onChange={(event) => setStatusFilter(event.target.value as RoomStatus | "")} aria-label="Lọc trạng thái">
                            <option value="">Tất cả trạng thái</option>
                            {roomStatuses.map((status) => <option key={status} value={status}>{statusLabel[status]}</option>)}
                        </select>
                    </div>

                    {roomsQuery.isLoading ? (
                        <div className="py-10 text-center text-sm text-slate-500">Đang tải danh sách phòng...</div>
                    ) : filteredRooms.length === 0 ? (
                        <EmptyState title="Không có phòng" description="Không có phòng phù hợp với bộ lọc hiện tại." />
                    ) : (
                        <div className="overflow-x-auto">
                            <table className="min-w-full divide-y divide-slate-200 text-left text-sm">
                                <thead className="bg-slate-50 text-slate-500">
                                    <tr>
                                        <th className="px-4 py-3 font-medium">Tên phòng</th>
                                        <th className="px-4 py-3 font-medium">Loại</th>
                                        <th className="px-4 py-3 font-medium">Tầng</th>
                                        <th className="px-4 py-3 font-medium">Sức chứa</th>
                                        <th className="px-4 py-3 font-medium">Trạng thái</th>
                                        <th className="px-4 py-3 font-medium">Mô tả</th>
                                        <th className="px-4 py-3 font-medium">Hành động</th>
                                    </tr>
                                </thead>
                                <tbody className="divide-y divide-slate-200 bg-white">
                                    {filteredRooms.map((room) => (
                                        <tr key={room.id}>
                                            <td className="px-4 py-3 font-medium text-slate-900">{room.name}</td>
                                            <td className="px-4 py-3">{room.roomTypeName}</td>
                                            <td className="px-4 py-3">{room.floor}</td>
                                            <td className="px-4 py-3">{room.capacity}</td>
                                            <td className="px-4 py-3"><Tag tone={statusTone[room.statusCode]}>{statusLabel[room.statusCode]}</Tag></td>
                                            <td className="max-w-[240px] truncate px-4 py-3 text-slate-600">{room.description || "-"}</td>
                                            <td className="px-4 py-3">
                                                <div className="flex flex-wrap gap-2">
                                                    <Button variant="outline" className="px-3 py-1.5 text-xs" onClick={() => openRoom(room, "view")}>Xem</Button>
                                                    <Button variant="outline" className="px-3 py-1.5 text-xs" onClick={() => openRoom(room, "edit")}>Sửa</Button>
                                                    <Button variant="ghost" className="px-3 py-1.5 text-xs text-rose-700" onClick={() => setDeleteRoomTarget(room)}>Xóa</Button>
                                                </div>
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>
                    )}
                </Card>
            ) : (
                <Card title="Danh sách loại phòng" right={<Button onClick={openCreateRoomType}>Thêm loại phòng</Button>}>
                    {roomTypesQuery.isLoading ? (
                        <div className="py-10 text-center text-sm text-slate-500">Đang tải danh sách loại phòng...</div>
                    ) : roomTypes.length === 0 ? (
                        <EmptyState title="Không có loại phòng" description="Chưa có loại phòng lưu trú nào." />
                    ) : (
                        <div className="overflow-x-auto">
                            <table className="min-w-full divide-y divide-slate-200 text-left text-sm">
                                <thead className="bg-slate-50 text-slate-500">
                                    <tr>
                                        <th className="px-4 py-3 font-medium">Tên loại</th>
                                        <th className="px-4 py-3 font-medium">Sức chứa</th>
                                        <th className="px-4 py-3 font-medium">Giá/ngày</th>
                                        <th className="px-4 py-3 font-medium">Trạng thái</th>
                                        <th className="px-4 py-3 font-medium">Mô tả</th>
                                        <th className="px-4 py-3 font-medium">Hành động</th>
                                    </tr>
                                </thead>
                                <tbody className="divide-y divide-slate-200 bg-white">
                                    {roomTypes.map((roomType) => (
                                        <tr key={roomType.id}>
                                            <td className="px-4 py-3">{roomType.name}</td>
                                            <td className="px-4 py-3">{roomType.defaultCapacity}</td>
                                            <td className="px-4 py-3">{formatCurrency(roomType.baseDailyPriceVnd)}</td>
                                            <td className="px-4 py-3"><Tag tone={roomType.isActive ? "green" : "red"}>{roomType.isActive ? "Đang áp dụng" : "Ngừng áp dụng"}</Tag></td>
                                            <td className="max-w-[260px] truncate px-4 py-3 text-slate-600">{roomType.description || "-"}</td>
                                            <td className="px-4 py-3">
                                                <div className="flex flex-wrap gap-2">
                                                    <Button variant="outline" className="px-3 py-1.5 text-xs" onClick={() => openRoomType(roomType, "view")}>Xem</Button>
                                                    <Button variant="outline" className="px-3 py-1.5 text-xs" onClick={() => openRoomType(roomType, "edit")}>Sửa</Button>
                                                    <Button variant="ghost" className="px-3 py-1.5 text-xs text-rose-700" onClick={() => setDeleteRoomTypeTarget(roomType)}>Xóa</Button>
                                                </div>
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>
                    )}
                </Card>
            )}

            <Modal isOpen={roomModalMode !== null} onClose={closeRoomModal} title={roomModalTitle}>
                <form className="space-y-4" onSubmit={roomForm.handleSubmit((values) => saveRoomMutation.mutate(values))}>
                    <div className="grid gap-4 md:grid-cols-2">
                        <Input label="Tên phòng" readOnly={roomReadOnly} {...roomForm.register("name")} error={roomForm.formState.errors.name?.message} />
                        <div className="flex flex-col gap-1.5">
                            <label className="text-[13px] font-medium text-slate-700" htmlFor="room-type-id">Loại phòng</label>
                            <select id="room-type-id" disabled={roomReadOnly} className="h-10 rounded-xl border border-slate-200 bg-white px-3 text-sm text-slate-900 disabled:bg-slate-50" {...roomForm.register("roomTypeId")}>
                                <option value="">Chọn loại phòng</option>
                                {activeRoomTypes.map((roomType) => <option key={roomType.id} value={roomType.id}>{roomType.name}</option>)}
                            </select>
                            {roomForm.formState.errors.roomTypeId && <p className="text-xs font-medium text-rose-600">{roomForm.formState.errors.roomTypeId.message}</p>}
                        </div>
                        <div className="flex flex-col gap-1.5">
                            <label className="text-[13px] font-medium text-slate-700" htmlFor="room-status">Trạng thái</label>
                            <select id="room-status" disabled={roomReadOnly} className="h-10 rounded-xl border border-slate-200 bg-white px-3 text-sm text-slate-900 disabled:bg-slate-50" {...roomForm.register("statusCode")}>
                                {roomStatuses.map((status) => <option key={status} value={status}>{statusLabel[status]}</option>)}
                            </select>
                        </div>
                        <Input label="Tầng" type="number" min={1} readOnly={roomReadOnly} {...roomForm.register("floor")} error={roomForm.formState.errors.floor?.message} />
                        <Input label="Sức chứa" type="number" min={1} readOnly={roomReadOnly} {...roomForm.register("capacity")} error={roomForm.formState.errors.capacity?.message} />
                    </div>
                    <Input label="Mô tả" readOnly={roomReadOnly} {...roomForm.register("description")} />
                    {roomModalMode === "edit" && roomForm.watch("statusCode") === "OCCUPIED" && (
                        <p className="rounded-xl bg-sky-50 px-3 py-2 text-sm text-sky-700">Trạng thái đang sử dụng thường được cập nhật theo nghiệp vụ lưu trú. Chỉ sửa thủ công khi cần điều chỉnh dữ liệu.</p>
                    )}
                    <div className="flex justify-end gap-3">
                        <Button variant="outline" onClick={closeRoomModal} disabled={saveRoomMutation.isPending}>{roomReadOnly ? "Đóng" : "Hủy"}</Button>
                        {!roomReadOnly && <Button type="submit" disabled={saveRoomMutation.isPending}>{saveRoomMutation.isPending ? "Đang lưu..." : "Lưu"}</Button>}
                    </div>
                </form>
            </Modal>

            <Modal isOpen={roomTypeModalMode !== null} onClose={closeRoomTypeModal} title={roomTypeModalTitle}>
                <form className="space-y-4" onSubmit={roomTypeForm.handleSubmit((values) => saveRoomTypeMutation.mutate(values))}>
                    <div className="grid gap-4 md:grid-cols-2">
                        <Input label="Tên loại" readOnly={roomTypeReadOnly} {...roomTypeForm.register("name")} error={roomTypeForm.formState.errors.name?.message} />
                        <Input label="Sức chứa mặc định" type="number" min={1} readOnly={roomTypeReadOnly} {...roomTypeForm.register("defaultCapacity")} error={roomTypeForm.formState.errors.defaultCapacity?.message} />
                        <Input label="Giá/ngày" type="number" min={0} readOnly={roomTypeReadOnly} {...roomTypeForm.register("baseDailyPriceVnd")} error={roomTypeForm.formState.errors.baseDailyPriceVnd?.message} />
                        <div className="flex flex-col gap-1.5">
                            <label className="text-[13px] font-medium text-slate-700" htmlFor="room-type-active">Trạng thái</label>
                            <select id="room-type-active" disabled={roomTypeReadOnly} className="h-10 rounded-xl border border-slate-200 bg-white px-3 text-sm text-slate-900 disabled:bg-slate-50" {...roomTypeForm.register("isActive")}>
                                <option value="true">Đang áp dụng</option>
                                <option value="false">Ngừng áp dụng</option>
                            </select>
                        </div>
                    </div>
                    <Input label="Mô tả" readOnly={roomTypeReadOnly} {...roomTypeForm.register("description")} />
                    <div className="flex justify-end gap-3">
                        <Button variant="outline" onClick={closeRoomTypeModal} disabled={saveRoomTypeMutation.isPending}>{roomTypeReadOnly ? "Đóng" : "Hủy"}</Button>
                        {!roomTypeReadOnly && <Button type="submit" disabled={saveRoomTypeMutation.isPending}>{saveRoomTypeMutation.isPending ? "Đang lưu..." : "Lưu"}</Button>}
                    </div>
                </form>
            </Modal>

            <Modal isOpen={deleteRoomTarget !== null} onClose={() => setDeleteRoomTarget(null)} title="Xóa phòng">
                <div className="space-y-4">
                    <p className="text-sm text-slate-600">Phòng sẽ được chuyển sang trạng thái ngừng áp dụng. Hành động này không xóa cứng dữ liệu.</p>
                    <div className="flex justify-end gap-3">
                        <Button variant="outline" onClick={() => setDeleteRoomTarget(null)} disabled={deleteRoomMutation.isPending}>Hủy</Button>
                        <Button variant="ghost" className="text-rose-700" disabled={deleteRoomMutation.isPending || !deleteRoomTarget} onClick={() => deleteRoomTarget && deleteRoomMutation.mutate(deleteRoomTarget)}>
                            {deleteRoomMutation.isPending ? "Đang xử lý..." : "Xóa"}
                        </Button>
                    </div>
                </div>
            </Modal>

            <Modal isOpen={deleteRoomTypeTarget !== null} onClose={() => setDeleteRoomTypeTarget(null)} title="Xóa loại phòng">
                <div className="space-y-4">
                    <p className="text-sm text-slate-600">Loại phòng sẽ được ngừng áp dụng nếu chưa được phòng nào tham chiếu.</p>
                    <div className="flex justify-end gap-3">
                        <Button variant="outline" onClick={() => setDeleteRoomTypeTarget(null)} disabled={deleteRoomTypeMutation.isPending}>Hủy</Button>
                        <Button variant="ghost" className="text-rose-700" disabled={deleteRoomTypeMutation.isPending || !deleteRoomTypeTarget} onClick={() => deleteRoomTypeTarget && deleteRoomTypeMutation.mutate(deleteRoomTypeTarget)}>
                            {deleteRoomTypeMutation.isPending ? "Đang xử lý..." : "Xóa"}
                        </Button>
                    </div>
                </div>
            </Modal>
        </div>
    );
}
