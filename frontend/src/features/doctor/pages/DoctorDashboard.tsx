import { Stethoscope, CheckCircle2, FileText, Pill, ArrowRight, Clock } from "lucide-react";
import { Card, MiniGridStats, AlertCard } from "~/components/molecules";
import { Button } from "~/components/atoms";
import { SkeletonLoader } from "~/shared/components/SkeletonLoader";
import { useQuery } from "@tanstack/react-query";
import { appointmentApi } from "~/shared/api/appointmentApi";
import { medicalRecordApi } from "~/shared/api/medicalRecordApi";
import { useAuth } from "~/features/auth/context/AuthContext";
import { Link } from "react-router-dom";

export function DoctorDashboard() {
    const { user } = useAuth();
    const { data: queueData, isLoading: queueLoading } = useQuery({
        queryKey: ["doctor-queue"],
        queryFn: () => appointmentApi.getVetQueue(),
    });

    const { data: medicalRecords, isLoading: recordsLoading } = useQuery({
        queryKey: ["doctor-medical-records", user?.id],
        queryFn: () => medicalRecordApi.getMedicalRecords(user?.id),
        enabled: !!user?.id,
    });

    const { data: todayAppointments, isLoading: todayLoading } = useQuery({
        queryKey: ["doctor-today-appointments", user?.id],
        queryFn: () => appointmentApi.listTodayAppointments({}),
    });

    const isLoading = queueLoading || recordsLoading || todayLoading;

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
    
    // Đã khám hôm nay (từ lịch hẹn hoàn tất của chính bác sĩ này)
    const todayCompleted = (todayAppointments ?? []).filter(
        (a: any) => a.assignedStaffId === user?.id && a.statusCode === "COMPLETED"
    ).length;

    // Bệnh án nháp
    const draftRecords = (medicalRecords ?? []).filter(
        (r: any) => r.recordStatus === "DRAFT"
    ).length;

    // Bệnh án hoàn tất hôm nay
    const todayStr = new Date().toDateString();
    const finalizedRecords = (medicalRecords ?? []).filter(
        (r: any) => r.recordStatus === "FINALIZED" && new Date(r.createdAt).toDateString() === todayStr
    ).length;

    // Alert cards: pets with abnormal vitals from queue entries
    const alertEntries = queue.filter((e: any) => e.alert || e.hasAlert);
    const alertCards = alertEntries.slice(0, 3).map((e: any) => ({
        pet: e.petName ?? "Không rõ",
        metric: e.alertMetric ?? "Cần theo dõi",
        note: e.alertNote ?? "Dấu hiệu bất thường",
        type: "warning" as const,
    }));

    // If no alert data, show generic placeholders
    const displayAlerts =
        alertCards.length > 0
            ? alertCards
            : [{ pet: "Tất cả ổn định", metric: "Không có cảnh báo", note: "Các chỉ số trong ngưỡng an toàn", type: "success" as const }];

    const upcomingPatients = queue.slice(0, 3);

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
                        label: "Bệnh án hoàn tất (Hôm nay)",
                        value: String(finalizedRecords),
                        hint: undefined,
                        icon: Pill,
                    },
                ]}
            />

            <div className="grid gap-6 lg:grid-cols-2">
                <Card title="Bệnh nhân tiếp theo">
                    {upcomingPatients.length > 0 ? (
                        <div className="space-y-4">
                            {upcomingPatients.map((patient: any) => (
                                <div key={patient.appointmentId} className="flex items-center justify-between rounded-2xl border border-gray-100 bg-gray-50 p-4 transition-colors hover:bg-gray-100">
                                    <div className="flex items-center gap-4">
                                        <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-blue-100 text-blue-700">
                                            <Clock className="h-6 w-6" />
                                        </div>
                                        <div>
                                            <h4 className="font-semibold text-gray-900">{patient.petName}</h4>
                                            <p className="text-sm text-gray-500">Chủ: {patient.ownerName}</p>
                                        </div>
                                    </div>
                                    <Link to={`/doctor/records/${patient.appointmentId}`}>
                                        <Button size="sm" variant="outline" className="gap-2">
                                            Khám ngay
                                            <ArrowRight className="h-4 w-4" />
                                        </Button>
                                    </Link>
                                </div>
                            ))}
                        </div>
                    ) : (
                        <div className="flex h-32 flex-col items-center justify-center rounded-2xl border border-dashed border-gray-200">
                            <Stethoscope className="mb-2 h-8 w-8 text-gray-300" />
                            <p className="text-gray-500">Hiện không có bệnh nhân nào đang chờ</p>
                        </div>
                    )}
                </Card>

                <Card title="Cảnh báo sinh hiệu">
                    <div className="flex flex-col gap-4">
                        {displayAlerts.map((alert, i) => (
                            <AlertCard
                                key={i}
                                pet={alert.pet}
                                metric={alert.metric}
                                note={alert.note}
                                type={alert.type}
                            />
                        ))}
                    </div>
                </Card>
            </div>
        </div>
    );
}
