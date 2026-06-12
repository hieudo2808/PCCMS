import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Loader2 } from "lucide-react";
import { useForm } from "react-hook-form";
import toast from "react-hot-toast";
import { z } from "zod";
import { Button, Input } from "~/components/atoms";
import { Card, DataTable, EmptyState } from "~/components/molecules";
import { groomingApi } from "~/features/grooming/api/groomingApi";

const serviceSchema = z.object({
    serviceCode: z.string().min(1, "Nhập mã dịch vụ").max(60),
    name: z.string().min(1, "Nhập tên dịch vụ").max(160),
    description: z.string().max(2000).optional(),
    basePriceVnd: z.coerce.number().min(0, "Giá phải >= 0"),
    durationMinutes: z.coerce.number().min(1, "Thời lượng phải > 0"),
});

const stationSchema = z.object({
    stationCode: z.string().min(1, "Nhập mã station").max(20),
    name: z.string().min(1, "Nhập tên station").max(80),
    isActive: z.boolean(),
});

type ServiceFormValues = z.infer<typeof serviceSchema>;
type StationFormValues = z.infer<typeof stationSchema>;

function formatCurrency(value?: number) {
    return new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND" }).format(
        value ?? 0
    );
}

export function GroomingAdminPanel() {
    const queryClient = useQueryClient();
    const servicesQuery = useQuery({
        queryKey: ["admin-grooming-services"],
        queryFn: () => groomingApi.getAdminServices(),
    });
    const stationsQuery = useQuery({
        queryKey: ["admin-grooming-stations"],
        queryFn: () => groomingApi.getAdminStations(),
    });

    const serviceForm = useForm<ServiceFormValues>({
        resolver: zodResolver(serviceSchema) as any,
        defaultValues: {
            serviceCode: "",
            name: "",
            description: "",
            basePriceVnd: 0,
            durationMinutes: 60,
        },
    });

    const stationForm = useForm<StationFormValues>({
        resolver: zodResolver(stationSchema),
        defaultValues: {
            stationCode: "",
            name: "",
            isActive: true,
        },
    });

    const createServiceMutation = useMutation({
        mutationFn: (data: ServiceFormValues) => groomingApi.createAdminService(data),
        onSuccess: () => {
            toast.success("Đã thêm dịch vụ làm đẹp");
            serviceForm.reset();
            queryClient.invalidateQueries({ queryKey: ["admin-grooming-services"] });
            queryClient.invalidateQueries({ queryKey: ["grooming-services"] });
        },
        onError: () => toast.error("Không thể thêm dịch vụ làm đẹp"),
    });

    const deactivateServiceMutation = useMutation({
        mutationFn: (id: string) => groomingApi.deactivateAdminService(id),
        onSuccess: () => {
            toast.success("Đã tắt dịch vụ làm đẹp");
            queryClient.invalidateQueries({ queryKey: ["admin-grooming-services"] });
            queryClient.invalidateQueries({ queryKey: ["grooming-services"] });
        },
        onError: () => toast.error("Không thể tắt dịch vụ làm đẹp"),
    });

    const createStationMutation = useMutation({
        mutationFn: (data: StationFormValues) => groomingApi.createStation(data),
        onSuccess: () => {
            toast.success("Đã thêm station làm đẹp");
            stationForm.reset({ stationCode: "", name: "", isActive: true });
            queryClient.invalidateQueries({ queryKey: ["admin-grooming-stations"] });
            queryClient.invalidateQueries({ queryKey: ["grooming-stations"] });
        },
        onError: () => toast.error("Không thể thêm station làm đẹp"),
    });

    const deactivateStationMutation = useMutation({
        mutationFn: (id: string) => groomingApi.deactivateStation(id),
        onSuccess: () => {
            toast.success("Đã tắt station làm đẹp");
            queryClient.invalidateQueries({ queryKey: ["admin-grooming-stations"] });
            queryClient.invalidateQueries({ queryKey: ["grooming-stations"] });
        },
        onError: () => toast.error("Không thể tắt station làm đẹp"),
    });

    if (servicesQuery.isLoading || stationsQuery.isLoading) {
        return (
            <div className="flex items-center gap-2 text-sm text-slate-500">
                <Loader2 className="h-4 w-4 animate-spin" /> Đang tải danh mục làm đẹp
            </div>
        );
    }

    if (servicesQuery.isError || stationsQuery.isError) {
        return (
            <EmptyState
                title="Không thể tải danh mục làm đẹp"
                description="Vui lòng thử lại sau."
            />
        );
    }

    const serviceRows =
        servicesQuery.data?.map((service) => [
            service.serviceCode,
            service.name,
            `${service.durationMinutes} phút`,
            formatCurrency(service.basePriceVnd),
            service.isActive ? "Đang bật" : "Đã tắt",
            <Button
                key={`service-${service.id}`}
                variant="outline"
                disabled={!service.isActive || deactivateServiceMutation.isPending}
                onClick={() => deactivateServiceMutation.mutate(service.id)}
            >
                Tắt
            </Button>,
        ]) ?? [];

    const stationRows =
        stationsQuery.data?.map((station) => [
            station.stationCode,
            station.name,
            station.isActive ? "Đang bật" : "Đã tắt",
            <Button
                key={`station-${station.id}`}
                variant="outline"
                disabled={!station.isActive || deactivateStationMutation.isPending}
                onClick={() => deactivateStationMutation.mutate(station.id)}
            >
                Tắt
            </Button>,
        ]) ?? [];

    return (
        <div className="grid gap-6 xl:grid-cols-2">
            <Card title="Dịch vụ làm đẹp">
                <form
                    className="mb-5 grid gap-3 md:grid-cols-2"
                    onSubmit={serviceForm.handleSubmit((data) =>
                        createServiceMutation.mutate(data)
                    )}
                >
                    <Input
                        label="Mã dịch vụ"
                        {...serviceForm.register("serviceCode")}
                        error={serviceForm.formState.errors.serviceCode?.message}
                    />
                    <Input
                        label="Tên dịch vụ"
                        {...serviceForm.register("name")}
                        error={serviceForm.formState.errors.name?.message}
                    />
                    <Input
                        label="Giá VND"
                        type="number"
                        {...serviceForm.register("basePriceVnd")}
                        error={serviceForm.formState.errors.basePriceVnd?.message}
                    />
                    <Input
                        label="Thời lượng phút"
                        type="number"
                        {...serviceForm.register("durationMinutes")}
                        error={serviceForm.formState.errors.durationMinutes?.message}
                    />
                    <Input
                        label="Mô tả"
                        className="md:col-span-2"
                        {...serviceForm.register("description")}
                        error={serviceForm.formState.errors.description?.message}
                    />
                    <Button
                        type="submit"
                        disabled={createServiceMutation.isPending}
                        className="md:col-span-2"
                    >
                        {createServiceMutation.isPending ? "Đang lưu..." : "Thêm dịch vụ làm đẹp"}
                    </Button>
                </form>
                {serviceRows.length === 0 ? (
                    <EmptyState
                        title="Chưa có dịch vụ làm đẹp"
                        description="Thêm dịch vụ để chủ nuôi có thể đặt lịch."
                    />
                ) : (
                    <DataTable
                        columns={["Mã", "Tên", "Thời lượng", "Giá", "Trạng thái", "Hành động"]}
                        rows={serviceRows}
                    />
                )}
            </Card>

            <Card title="Khu làm đẹp">
                <form
                    className="mb-5 grid gap-3 md:grid-cols-2"
                    onSubmit={stationForm.handleSubmit((data) =>
                        createStationMutation.mutate(data)
                    )}
                >
                    <Input
                        label="Mã station"
                        {...stationForm.register("stationCode")}
                        error={stationForm.formState.errors.stationCode?.message}
                    />
                    <Input
                        label="Tên station"
                        {...stationForm.register("name")}
                        error={stationForm.formState.errors.name?.message}
                    />
                    <label className="flex items-center gap-2 text-sm text-slate-700">
                        <input type="checkbox" {...stationForm.register("isActive")} />
                        Đang hoạt động
                    </label>
                    <Button type="submit" disabled={createStationMutation.isPending}>
                        {createStationMutation.isPending ? "Đang lưu..." : "Thêm station"}
                    </Button>
                </form>
                {stationRows.length === 0 ? (
                    <EmptyState
                        title="Chưa có station"
                        description="Thêm station để nhân viên có thể gán lịch làm đẹp."
                    />
                ) : (
                    <DataTable
                        columns={["Mã", "Tên", "Trạng thái", "Hành động"]}
                        rows={stationRows}
                    />
                )}
            </Card>
        </div>
    );
}
