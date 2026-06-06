import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Loader2, Plus, Save } from "lucide-react";
import { useForm } from "react-hook-form";
import toast from "react-hot-toast";
import { z } from "zod";
import { Button, Input, Tag, Textarea } from "~/components/atoms";
import { Card, EmptyState, SummaryRow } from "~/components/molecules";
import { roomAdminApi } from "~/features/boarding/api/boardingApi";
import type { RoomRequest, RoomStatus, RoomTypeRequest } from "~/types/boarding";

const roomTypeSchema = z.object({
  code: z.string().min(1).max(60),
  name: z.string().min(1).max(120),
  defaultCapacity: z.number().min(1),
  baseDailyPriceVnd: z.number().min(0),
  description: z.string().optional(),
});

const roomSchema = z.object({
  roomCode: z.string().min(1).max(60),
  name: z.string().min(1).max(120),
  roomTypeId: z.string().min(1),
  floor: z.number().min(1),
  capacity: z.number().min(1),
  statusCode: z.enum(["AVAILABLE", "OCCUPIED", "MAINTENANCE", "INACTIVE"]),
  description: z.string().optional(),
});

type RoomTypeFormValues = z.infer<typeof roomTypeSchema>;
type RoomFormValues = z.infer<typeof roomSchema>;

const statusTone: Record<RoomStatus, "green" | "amber" | "red" | "blue" | "default"> = {
  AVAILABLE: "green",
  OCCUPIED: "blue",
  MAINTENANCE: "amber",
  INACTIVE: "red",
};

function formatCurrency(value?: number) {
  return new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND" }).format(value ?? 0);
}

export function RoomsPage() {
  const queryClient = useQueryClient();
  const roomTypesQuery = useQuery({ queryKey: ["room-types"], queryFn: () => roomAdminApi.getRoomTypes() });
  const roomsQuery = useQuery({ queryKey: ["rooms"], queryFn: () => roomAdminApi.getRooms() });

  const roomTypeForm = useForm<RoomTypeFormValues>({
    resolver: zodResolver(roomTypeSchema),
    defaultValues: { code: "", name: "", defaultCapacity: 1, baseDailyPriceVnd: 0, description: "" },
  });

  const roomForm = useForm<RoomFormValues>({
    resolver: zodResolver(roomSchema),
    defaultValues: { roomCode: "", name: "", roomTypeId: "", floor: 1, capacity: 1, statusCode: "AVAILABLE", description: "" },
  });

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ["room-types"] });
    queryClient.invalidateQueries({ queryKey: ["rooms"] });
  };

  const createRoomTypeMutation = useMutation({
    mutationFn: (values: RoomTypeFormValues) => roomAdminApi.createRoomType(values as RoomTypeRequest),
    onSuccess: () => {
      toast.success("Đã tạo loại phòng");
      roomTypeForm.reset();
      invalidate();
    },
    onError: () => toast.error("Không thể tạo loại phòng"),
  });

  const createRoomMutation = useMutation({
    mutationFn: (values: RoomFormValues) => roomAdminApi.createRoom(values as RoomRequest),
    onSuccess: () => {
      toast.success("Đã tạo phòng");
      roomForm.reset();
      invalidate();
    },
    onError: () => toast.error("Không thể tạo phòng"),
  });

  const updateStatusMutation = useMutation({
    mutationFn: ({ id, statusCode }: { id: string; statusCode: RoomStatus }) => roomAdminApi.updateRoomStatus(id, statusCode),
    onSuccess: invalidate,
    onError: () => toast.error("Không thể đổi trạng thái phòng"),
  });

  const roomTypes = roomTypesQuery.data ?? [];
  const rooms = roomsQuery.data?.content ?? [];

  return (
    <div className="space-y-6">
      <div className="grid gap-6 xl:grid-cols-2">
        <Card title="Thêm loại phòng" subtitle="Giá/ngày sẽ được dùng để tính tạm tính và hóa đơn checkout.">
          <form className="space-y-4" onSubmit={roomTypeForm.handleSubmit((values) => createRoomTypeMutation.mutate(values))}>
            <div className="grid gap-4 md:grid-cols-2">
              <Input label="Mã loại phòng" error={roomTypeForm.formState.errors.code?.message} {...roomTypeForm.register("code")} />
              <Input label="Tên loại phòng" error={roomTypeForm.formState.errors.name?.message} {...roomTypeForm.register("name")} />
              <Input type="number" label="Suc chua mac dinh" error={roomTypeForm.formState.errors.defaultCapacity?.message} {...roomTypeForm.register("defaultCapacity", { valueAsNumber: true })} />
              <Input type="number" label="Giá/ngày" error={roomTypeForm.formState.errors.baseDailyPriceVnd?.message} {...roomTypeForm.register("baseDailyPriceVnd", { valueAsNumber: true })} />
            </div>
            <Textarea label="Mo ta" rows={3} {...roomTypeForm.register("description")} />
            <Button type="submit" disabled={createRoomTypeMutation.isPending}>
              <span className="inline-flex items-center gap-2">
                {createRoomTypeMutation.isPending ? <Loader2 className="h-4 w-4 animate-spin" /> : <Plus className="h-4 w-4" />}
                Tạo loại phòng
              </span>
            </Button>
          </form>
        </Card>

        <Card title="Thêm phòng" subtitle="Phòng cụ thể sẽ được nhân viên gán khi duyệt booking.">
          <form className="space-y-4" onSubmit={roomForm.handleSubmit((values) => createRoomMutation.mutate(values))}>
            <div className="grid gap-4 md:grid-cols-2">
              <Input label="Mã phòng" error={roomForm.formState.errors.roomCode?.message} {...roomForm.register("roomCode")} />
              <Input label="Tên phòng" error={roomForm.formState.errors.name?.message} {...roomForm.register("name")} />
              <label className="flex flex-col gap-1.5 text-[13px] font-medium text-slate-700">
                Loại phòng
                <select className="h-10 rounded-xl border border-slate-200 bg-white px-3 text-sm" {...roomForm.register("roomTypeId")}>
                  <option value="">Chọn loại phòng</option>
                  {roomTypes.map((type) => <option key={type.id} value={type.id}>{type.name}</option>)}
                </select>
              </label>
              <label className="flex flex-col gap-1.5 text-[13px] font-medium text-slate-700">
                Trạng thái
                <select className="h-10 rounded-xl border border-slate-200 bg-white px-3 text-sm" {...roomForm.register("statusCode")}>
                  <option value="AVAILABLE">AVAILABLE</option>
                  <option value="MAINTENANCE">MAINTENANCE</option>
                  <option value="INACTIVE">INACTIVE</option>
                </select>
              </label>
              <Input type="number" label="Tang" error={roomForm.formState.errors.floor?.message} {...roomForm.register("floor", { valueAsNumber: true })} />
              <Input type="number" label="Suc chua" error={roomForm.formState.errors.capacity?.message} {...roomForm.register("capacity", { valueAsNumber: true })} />
            </div>
            <Textarea label="Mo ta" rows={3} {...roomForm.register("description")} />
            <Button type="submit" disabled={createRoomMutation.isPending}>
              <span className="inline-flex items-center gap-2">
                {createRoomMutation.isPending ? <Loader2 className="h-4 w-4 animate-spin" /> : <Save className="h-4 w-4" />}
                Tạo phòng
              </span>
            </Button>
          </form>
        </Card>
      </div>

      <Card title="Danh sách loại phòng">
        {roomTypesQuery.isLoading ? (
          <div className="flex items-center gap-2 text-sm text-slate-500"><Loader2 className="h-4 w-4 animate-spin" /> Đang tải loại phòng</div>
        ) : roomTypes.length === 0 ? (
          <EmptyState title="Chưa có loại phòng" description="Tạo loại phòng trước khi thêm phòng." />
        ) : (
          <div className="grid gap-4 md:grid-cols-3">
            {roomTypes.map((type) => (
              <div key={type.id} className="rounded-2xl border border-slate-200 p-4">
                <div className="flex items-start justify-between gap-3">
                  <div>
                    <p className="font-semibold">{type.name}</p>
                    <p className="mt-1 text-sm text-slate-500">{type.code}</p>
                  </div>
                  <Tag tone="blue">{type.defaultCapacity}</Tag>
                </div>
                <p className="mt-3 text-sm font-semibold text-emerald-700">{formatCurrency(type.baseDailyPriceVnd)}/ngày</p>
              </div>
            ))}
          </div>
        )}
      </Card>

      <Card title="Kho phòng lưu trú">
        {roomsQuery.isLoading ? (
          <div className="flex items-center gap-2 text-sm text-slate-500"><Loader2 className="h-4 w-4 animate-spin" /> Đang tải phòng</div>
        ) : rooms.length === 0 ? (
          <EmptyState title="Chưa có phòng" description="Tạo phòng để nhận booking lưu trú." />
        ) : (
          <div className="grid gap-4 lg:grid-cols-3">
            {rooms.map((room) => (
              <div key={room.id} className="rounded-2xl border border-slate-200 p-4">
                <div className="flex items-start justify-between gap-3">
                  <div>
                    <p className="font-semibold">{room.roomCode} - {room.name}</p>
                    <p className="mt-1 text-sm text-slate-500">{room.roomTypeName}</p>
                  </div>
                  <Tag tone={statusTone[room.statusCode]}>{room.statusCode}</Tag>
                </div>
                <div className="mt-4 space-y-2 text-sm">
                  <SummaryRow label="Tang" value={room.floor} />
                  <SummaryRow label="Suc chua" value={room.capacity} />
                </div>
                <div className="mt-4 flex flex-wrap gap-2">
                  {(["AVAILABLE", "MAINTENANCE", "INACTIVE"] as RoomStatus[]).map((statusCode) => (
                    <Button
                      key={statusCode}
                      variant={room.statusCode === statusCode ? "secondary" : "outline"}
                      disabled={updateStatusMutation.isPending}
                      onClick={() => updateStatusMutation.mutate({ id: room.id, statusCode })}
                    >
                      {statusCode}
                    </Button>
                  ))}
                </div>
              </div>
            ))}
          </div>
        )}
      </Card>
    </div>
  );
}
