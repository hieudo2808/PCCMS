import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { toast } from "react-hot-toast";
import { Button, Input, Select, Textarea } from "~/components/atoms";
import { Tag } from "~/components/atoms/Tag";
import { Card, DataTable, EmptyState, Modal } from "~/components/molecules";
import { catalogApi } from "../api/catalogApi";
import type { RoomResponse, RoomStatus, RoomTypeResponse } from "~/types/catalog";

const STATUS_OPTIONS: { value: RoomStatus; label: string; tone: "green" | "blue" | "red" | "amber" }[] = [
  { value: "AVAILABLE", label: "Trống", tone: "green" },
  { value: "OCCUPIED", label: "Đang sử dụng", tone: "blue" },
  { value: "MAINTENANCE", label: "Bảo trì", tone: "red" },
  { value: "INACTIVE", label: "Ngừng áp dụng", tone: "amber" },
];

const roomSchema = z.object({
  roomCode: z.string().min(1, "Vui lòng nhập mã phòng"),
  name: z.string().min(1, "Vui lòng nhập tên phòng"),
  roomTypeId: z.string().min(1, "Vui lòng chọn loại phòng"),
  capacity: z.coerce.number().min(1, "Sức chứa phải >= 1"),
  statusCode: z.enum(["AVAILABLE", "OCCUPIED", "MAINTENANCE", "INACTIVE"]),
  floor: z.coerce.number().min(1).optional(),
  description: z.string().optional(),
});

const roomTypeSchema = z.object({
  code: z.string().min(1, "Vui lòng nhập mã loại phòng"),
  name: z.string().min(1, "Vui lòng nhập tên loại phòng"),
  defaultCapacity: z.coerce.number().min(1, "Sức chứa mặc định phải >= 1"),
  baseDailyPriceVnd: z.coerce.number().min(0, "Giá phải >= 0"),
  description: z.string().optional(),
  isActive: z.boolean(),
});

type RoomFormValues = z.infer<typeof roomSchema>;
type RoomTypeFormValues = z.infer<typeof roomTypeSchema>;
type Tab = "rooms" | "roomTypes";

function statusTone(status: RoomStatus): "green" | "blue" | "red" | "amber" {
  return STATUS_OPTIONS.find((s) => s.value === status)?.tone ?? "green";
}

export function RoomsPage() {
  const queryClient = useQueryClient();
  const [tab, setTab] = useState<Tab>("rooms");
  const [statusFilter, setStatusFilter] = useState<RoomStatus | "">("");
  const [roomTypeFilter, setRoomTypeFilter] = useState("");
  const [roomModal, setRoomModal] = useState<"create" | "edit" | null>(null);
  const [roomTypeModal, setRoomTypeModal] = useState<"create" | "edit" | null>(null);
  const [editingRoom, setEditingRoom] = useState<RoomResponse | null>(null);
  const [editingRoomType, setEditingRoomType] = useState<RoomTypeResponse | null>(null);

  const { data: roomTypes = [] } = useQuery({
    queryKey: ["room-types", "active"],
    queryFn: () => catalogApi.listRoomTypes(true),
  });

  const { data: allRoomTypes = [], isLoading: roomTypesLoading } = useQuery({
    queryKey: ["room-types", "all"],
    queryFn: () => catalogApi.listRoomTypes(false),
    enabled: tab === "roomTypes",
  });

  const { data: roomsPage, isLoading: roomsLoading, isError: roomsError } = useQuery({
    queryKey: ["rooms", statusFilter, roomTypeFilter],
    queryFn: () =>
      catalogApi.listRooms({
        page: 1,
        size: 50,
        statusCode: statusFilter || undefined,
        roomTypeId: roomTypeFilter || undefined,
      }),
    enabled: tab === "rooms",
  });

  const roomForm = useForm<RoomFormValues>({
    resolver: zodResolver(roomSchema) as never,
    defaultValues: {
      roomCode: "",
      name: "",
      roomTypeId: "",
      capacity: 1,
      statusCode: "AVAILABLE",
      floor: 1,
      description: "",
    },
  });

  const roomTypeForm = useForm<RoomTypeFormValues>({
    resolver: zodResolver(roomTypeSchema) as never,
    defaultValues: {
      code: "",
      name: "",
      defaultCapacity: 1,
      baseDailyPriceVnd: 0,
      description: "",
      isActive: true,
    },
  });

  const createRoomMutation = useMutation({
    mutationFn: catalogApi.createRoom,
    onSuccess: () => {
      toast.success("Thêm phòng thành công");
      closeRoomModal();
      queryClient.invalidateQueries({ queryKey: ["rooms"] });
    },
    onError: () => toast.error("Không thể thêm phòng"),
  });

  const updateRoomMutation = useMutation({
    mutationFn: ({ id, data }: { id: string; data: RoomFormValues }) => catalogApi.updateRoom(id, data),
    onSuccess: () => {
      toast.success("Cập nhật phòng thành công");
      closeRoomModal();
      queryClient.invalidateQueries({ queryKey: ["rooms"] });
    },
    onError: () => toast.error("Không thể cập nhật phòng"),
  });

  const deleteRoomMutation = useMutation({
    mutationFn: catalogApi.deleteRoom,
    onSuccess: () => {
      toast.success("Đã ngừng áp dụng phòng");
      queryClient.invalidateQueries({ queryKey: ["rooms"] });
    },
    onError: () => toast.error("Không thể xóa phòng"),
  });

  const createRoomTypeMutation = useMutation({
    mutationFn: catalogApi.createRoomType,
    onSuccess: () => {
      toast.success("Thêm loại phòng thành công");
      closeRoomTypeModal();
      queryClient.invalidateQueries({ queryKey: ["room-types"] });
    },
    onError: () => toast.error("Không thể thêm loại phòng"),
  });

  const updateRoomTypeMutation = useMutation({
    mutationFn: ({ id, data }: { id: string; data: RoomTypeFormValues }) => catalogApi.updateRoomType(id, data),
    onSuccess: () => {
      toast.success("Cập nhật loại phòng thành công");
      closeRoomTypeModal();
      queryClient.invalidateQueries({ queryKey: ["room-types"] });
    },
    onError: () => toast.error("Không thể cập nhật loại phòng"),
  });

  const deleteRoomTypeMutation = useMutation({
    mutationFn: catalogApi.deleteRoomType,
    onSuccess: () => {
      toast.success("Đã ngừng áp dụng loại phòng");
      queryClient.invalidateQueries({ queryKey: ["room-types"] });
    },
    onError: () => toast.error("Không thể xóa loại phòng"),
  });

  function closeRoomModal() {
    setRoomModal(null);
    setEditingRoom(null);
    roomForm.reset();
  }

  function closeRoomTypeModal() {
    setRoomTypeModal(null);
    setEditingRoomType(null);
    roomTypeForm.reset();
  }

  function openCreateRoom() {
    roomForm.reset({
      roomCode: "",
      name: "",
      roomTypeId: roomTypes[0]?.id ?? "",
      capacity: roomTypes[0]?.defaultCapacity ?? 1,
      statusCode: "AVAILABLE",
      floor: 1,
      description: "",
    });
    setEditingRoom(null);
    setRoomModal("create");
  }

  function openEditRoom(room: RoomResponse) {
    setEditingRoom(room);
    roomForm.reset({
      roomCode: room.roomCode,
      name: room.name,
      roomTypeId: room.roomTypeId,
      capacity: room.capacity,
      statusCode: room.statusCode,
      floor: room.floor,
      description: room.description || "",
    });
    setRoomModal("edit");
  }

  function openCreateRoomType() {
    roomTypeForm.reset({
      code: "",
      name: "",
      defaultCapacity: 1,
      baseDailyPriceVnd: 0,
      description: "",
      isActive: true,
    });
    setEditingRoomType(null);
    setRoomTypeModal("create");
  }

  function openEditRoomType(rt: RoomTypeResponse) {
    setEditingRoomType(rt);
    roomTypeForm.reset({
      code: rt.code,
      name: rt.name,
      defaultCapacity: rt.defaultCapacity,
      baseDailyPriceVnd: rt.baseDailyPriceVnd,
      description: rt.description || "",
      isActive: rt.isActive,
    });
    setRoomTypeModal("edit");
  }

  function onSubmitRoom(data: RoomFormValues) {
    if (roomModal === "edit" && editingRoom) {
      updateRoomMutation.mutate({ id: editingRoom.id, data });
    } else {
      createRoomMutation.mutate(data);
    }
  }

  function onSubmitRoomType(data: RoomTypeFormValues) {
    if (roomTypeModal === "edit" && editingRoomType) {
      updateRoomTypeMutation.mutate({ id: editingRoomType.id, data });
    } else {
      createRoomTypeMutation.mutate(data);
    }
  }

  const rooms = (roomsPage?.content ?? []).filter((r) => r.statusCode !== "INACTIVE");

  return (
    <div className="space-y-6">
      <div className="flex gap-2">
        <Button variant={tab === "rooms" ? "primary" : "outline"} onClick={() => setTab("rooms")}>
          Phòng lưu trú
        </Button>
        <Button variant={tab === "roomTypes" ? "primary" : "outline"} onClick={() => setTab("roomTypes")}>
          Loại phòng
        </Button>
      </div>

      {tab === "rooms" && (
        <Card
          title="Quản lý phòng lưu trú"
          right={
            <div className="flex flex-wrap items-center gap-2">
              <select
                className="rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm"
                value={roomTypeFilter}
                onChange={(e) => setRoomTypeFilter(e.target.value)}
              >
                <option value="">Tất cả loại phòng</option>
                {roomTypes.map((rt) => (
                  <option key={rt.id} value={rt.id}>
                    {rt.name}
                  </option>
                ))}
              </select>
              <select
                className="rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm"
                value={statusFilter}
                onChange={(e) => setStatusFilter(e.target.value as RoomStatus | "")}
              >
                <option value="">Tất cả trạng thái</option>
                {STATUS_OPTIONS.map((s) => (
                  <option key={s.value} value={s.value}>
                    {s.label}
                  </option>
                ))}
              </select>
              <Button onClick={openCreateRoom}>Thêm phòng</Button>
            </div>
          }
        >
          {roomsLoading ? (
            <div className="flex justify-center p-8">
              <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-indigo-600" />
            </div>
          ) : roomsError ? (
            <EmptyState title="Lỗi" description="Không thể tải danh sách phòng" />
          ) : rooms.length === 0 ? (
            <EmptyState title="Trống" description="Chưa có phòng lưu trú nào" />
          ) : (
            <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
              {rooms.map((room) => (
                <Card key={room.id} className="h-full transition hover:shadow-md">
                  <div className="flex items-start justify-between">
                    <div>
                      <p className="text-xl font-semibold">{room.roomCode}</p>
                      <p className="mt-1 text-sm font-medium text-slate-800">{room.name}</p>
                      <p className="mt-1 text-sm text-slate-500">{room.roomTypeName}</p>
                      <p className="mt-1 text-xs text-slate-400">
                        Tầng {room.floor} · Sức chứa {room.capacity}
                      </p>
                      {room.description && <p className="mt-2 text-xs text-slate-500">{room.description}</p>}
                    </div>
                    <Tag tone={statusTone(room.statusCode)}>{room.statusLabel}</Tag>
                  </div>
                  <div className="mt-4 flex gap-2">
                    <Button variant="outline" className="px-2 py-1 h-auto text-xs" onClick={() => openEditRoom(room)}>
                      Sửa
                    </Button>
                    {room.statusCode !== "OCCUPIED" && (
                      <Button
                        variant="outline"
                        className="px-2 py-1 h-auto text-xs"
                        onClick={() => {
                          if (confirm(`Ngừng áp dụng phòng "${room.name}"?`)) {
                            deleteRoomMutation.mutate(room.id);
                          }
                        }}
                      >
                        Xóa
                      </Button>
                    )}
                  </div>
                </Card>
              ))}
            </div>
          )}
        </Card>
      )}

      {tab === "roomTypes" && (
        <Card
          title="Quản lý loại phòng"
          right={<Button onClick={openCreateRoomType}>Thêm loại phòng</Button>}
        >
          {roomTypesLoading ? (
            <div className="flex justify-center p-8">
              <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-indigo-600" />
            </div>
          ) : allRoomTypes.length === 0 ? (
            <EmptyState title="Trống" description="Chưa có loại phòng nào" />
          ) : (
            <DataTable
              columns={["Mã", "Tên", "Sức chứa mặc định", "Giá/ngày", "Trạng thái", "Hành động"]}
              rows={allRoomTypes.map((rt) => [
                rt.code,
                rt.name,
                rt.defaultCapacity.toString(),
                rt.baseDailyPriceVnd.toLocaleString("vi-VN"),
                rt.isActive ? <Tag tone="green">Đang áp dụng</Tag> : <Tag tone="amber">Ngừng áp dụng</Tag>,
                <div key={rt.id} className="flex gap-1">
                  <Button variant="outline" className="px-2 py-1 h-auto text-xs" onClick={() => openEditRoomType(rt)}>
                    Sửa
                  </Button>
                  {rt.isActive && (
                    <Button
                      variant="outline"
                      className="px-2 py-1 h-auto text-xs"
                      onClick={() => {
                        if (confirm(`Ngừng áp dụng loại phòng "${rt.name}"?`)) {
                          deleteRoomTypeMutation.mutate(rt.id);
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
        </Card>
      )}

      <Modal isOpen={roomModal !== null} onClose={closeRoomModal} title={roomModal === "edit" ? "Sửa phòng" : "Thêm phòng mới"}>
        <form onSubmit={roomForm.handleSubmit(onSubmitRoom)} className="space-y-4">
          <Input label="Mã phòng" {...roomForm.register("roomCode")} error={roomForm.formState.errors.roomCode?.message} />
          <Input label="Tên phòng" {...roomForm.register("name")} error={roomForm.formState.errors.name?.message} />
          <Select
            label="Loại phòng"
            placeholder="Chọn loại phòng"
            options={roomTypes.map((rt) => ({ value: rt.id, label: rt.name }))}
            {...roomForm.register("roomTypeId")}
            error={roomForm.formState.errors.roomTypeId?.message}
          />
          <div className="grid grid-cols-2 gap-4">
            <Input label="Sức chứa" type="number" {...roomForm.register("capacity")} error={roomForm.formState.errors.capacity?.message} />
            <Input label="Tầng" type="number" {...roomForm.register("floor")} />
          </div>
          <Select
            label="Trạng thái"
            options={STATUS_OPTIONS.map((s) => ({ value: s.value, label: s.label }))}
            {...roomForm.register("statusCode")}
            error={roomForm.formState.errors.statusCode?.message}
          />
          <Textarea label="Ghi chú" {...roomForm.register("description")} />
          <div className="flex justify-end gap-2">
            <Button type="button" variant="outline" onClick={closeRoomModal}>Hủy</Button>
            <Button type="submit" disabled={createRoomMutation.isPending || updateRoomMutation.isPending}>
              Lưu
            </Button>
          </div>
        </form>
      </Modal>

      <Modal
        isOpen={roomTypeModal !== null}
        onClose={closeRoomTypeModal}
        title={roomTypeModal === "edit" ? "Sửa loại phòng" : "Thêm loại phòng"}
      >
        <form onSubmit={roomTypeForm.handleSubmit(onSubmitRoomType)} className="space-y-4">
          <Input label="Mã loại phòng" {...roomTypeForm.register("code")} error={roomTypeForm.formState.errors.code?.message} />
          <Input label="Tên loại phòng" {...roomTypeForm.register("name")} error={roomTypeForm.formState.errors.name?.message} />
          <div className="grid grid-cols-2 gap-4">
            <Input label="Sức chứa mặc định" type="number" {...roomTypeForm.register("defaultCapacity")} error={roomTypeForm.formState.errors.defaultCapacity?.message} />
            <Input label="Giá/ngày (VND)" type="number" {...roomTypeForm.register("baseDailyPriceVnd")} error={roomTypeForm.formState.errors.baseDailyPriceVnd?.message} />
          </div>
          <Textarea label="Mô tả" {...roomTypeForm.register("description")} />
          <label className="flex items-center gap-2 text-sm text-slate-700">
            <input type="checkbox" {...roomTypeForm.register("isActive")} />
            Đang áp dụng
          </label>
          <div className="flex justify-end gap-2">
            <Button type="button" variant="outline" onClick={closeRoomTypeModal}>Hủy</Button>
            <Button type="submit" disabled={createRoomTypeMutation.isPending || updateRoomTypeMutation.isPending}>
              Lưu
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
