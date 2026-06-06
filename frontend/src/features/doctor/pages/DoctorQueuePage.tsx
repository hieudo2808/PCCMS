import { useQuery } from "@tanstack/react-query";
import { Tag } from "~/components/atoms";
import { Card, DataTable, EmptyState } from "~/components/molecules";
import { appointmentApi } from "~/shared/api/appointmentApi";
import { hasAccessToken } from "~/shared/auth/tokenStorage";
import { useAuth } from "~/features/auth/context/AuthContext";
import type { QueueEntryResponse } from "~/types/appointment";

function priorityTone(symptom: string | null): "red" | "amber" | "green" {
    if (!symptom) return "green";
    const urgent = /nôn|bỏ ăn|sốt|chảy máu|khó thở/i;
    if (urgent.test(symptom)) return "red";
    if (symptom.length > 20) return "amber";
    return "green";
}

function priorityLabel(symptom: string | null): string {
    const tone = priorityTone(symptom);
    if (tone === "red") return "Cao";
    if (tone === "amber") return "Trung bình";
    return "Thấp";
}

export function DoctorQueuePage() {
    const { isAuthenticated, user } = useAuth();
    const canFetch = isAuthenticated && hasAccessToken() && Boolean(user);

    const { data: queue = [], isLoading } = useQuery({
        queryKey: ["appointments", "queue"],
        queryFn: () => appointmentApi.getVetQueue(),
        enabled: canFetch,
        refetchInterval: 30_000,
    });

    if (isLoading) {
        return (
            <div className="flex justify-center p-12">
                <div className="h-8 w-8 animate-spin rounded-full border-b-2 border-indigo-600" />
            </div>
        );
    }

    if (queue.length === 0) {
        return (
            <EmptyState
                title="Danh sách chờ khám"
                description="Chưa có thú cưng nào được tiếp nhận hôm nay."
            />
        );
    }

    const rows = queue.map((entry: QueueEntryResponse) => [
        entry.queueNumber,
        entry.petName,
        entry.ownerName,
        new Date(entry.checkedInAt).toLocaleTimeString("vi-VN", {
            hour: "2-digit",
            minute: "2-digit",
        }),
        entry.symptomText ?? "—",
        <Tag tone={priorityTone(entry.symptomText)}>{priorityLabel(entry.symptomText)}</Tag>,
        "Mở bệnh án",
    ]);

    return (
        <div className="space-y-6">
            <Card title="Danh sách chờ khám">
                <DataTable
                    columns={[
                        "Thứ tự",
                        "Thú cưng",
                        "Chủ nuôi",
                        "Giờ nhận",
                        "Triệu chứng ban đầu",
                        "Mức ưu tiên",
                        "Hành động",
                    ]}
                    rows={rows}
                />
            </Card>
        </div>
    );
}
