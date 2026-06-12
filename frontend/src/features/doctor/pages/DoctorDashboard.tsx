import { Stethoscope, CheckCircle2, FileText, Pill } from "lucide-react";
import { Card, MiniGridStats, AlertCard } from "~/components/molecules";
import { SkeletonLoader } from "~/shared/components/SkeletonLoader";
import { useQuery } from "@tanstack/react-query";
import { appointmentApi } from "~/shared/api/appointmentApi";
import { medicalRecordApi } from "~/shared/api/medicalRecordApi";

export function DoctorDashboard() {
    const { data: queueData, isLoading: queueLoading } = useQuery({
        queryKey: ["doctor-queue"],
        queryFn: () => appointmentApi.getVetQueue(),
    });

    const { data: medicalRecords, isLoading: recordsLoading } = useQuery({
        queryKey: ["doctor-medical-records"],
        queryFn: () => medicalRecordApi.getMedicalRecords(),
    });

    const isLoading = queueLoading && recordsLoading;

    if (isLoading) {
        return (
            <div className="space-y-6">
                <SkeletonLoader className="h-28 w-full rounded-2xl" />
                <SkeletonLoader className="h-48 w-full rounded-2xl" />
            </div>
        );
    }

    const queue = queueData ?? [];
    const waitingCount = queue.length;
    const todayCompleted = queue.filter(
        (e: any) => e.statusCode === "COMPLETED" || e.isCompleted
    ).length;
    const draftRecords = (medicalRecords ?? []).filter(
        (r: any) => r.statusCode === "DRAFT" || r.status === "DRAFT"
    ).length;
    const prescriptionsCount = (medicalRecords ?? []).filter(
        (r: any) => r.prescriptionCount > 0 || r.prescriptions?.length > 0
    ).length;

    // Alert cards: pets with abnormal vitals from queue entries
    const alertEntries = queue.filter((e: any) => e.alert || e.hasAlert);
    const alertCards = alertEntries.slice(0, 3).map((e: any) => ({
        pet: e.petName ?? "Không rõ",
        metric: e.alertMetric ?? "Cần theo dõi",
        note: e.alertNote ?? "Dấu hiệu bất thường",
    }));

    // If no alert data, show generic placeholders
    const displayAlerts =
        alertCards.length > 0
            ? alertCards
            : [{ pet: "—", metric: "Không có cảnh báo", note: "Tất cả bệnh nhân ổn định" }];

    return (
        <div className="space-y-6">
            <MiniGridStats
                items={[
                    {
                        label: "Đang chờ khám",
                        value: String(waitingCount),
                        hint: undefined,
                        icon: Stethoscope,
                    },
                    {
                        label: "Đã khám hôm nay",
                        value: String(todayCompleted),
                        hint: undefined,
                        icon: CheckCircle2,
                    },
                    {
                        label: "Bệnh án nháp",
                        value: String(draftRecords),
                        hint: draftRecords > 0 ? "Cần hoàn tất trước cuối ca" : undefined,
                        icon: FileText,
                    },
                    {
                        label: "Đơn thuốc đã kê",
                        value: String(prescriptionsCount),
                        hint: undefined,
                        icon: Pill,
                    },
                ]}
            />
            <Card title="Cảnh báo sinh hiệu">
                <div className="grid gap-4 lg:grid-cols-3">
                    {displayAlerts.map((alert, i) => (
                        <AlertCard
                            key={i}
                            pet={alert.pet}
                            metric={alert.metric}
                            note={alert.note}
                        />
                    ))}
                </div>
            </Card>
        </div>
    );
}
