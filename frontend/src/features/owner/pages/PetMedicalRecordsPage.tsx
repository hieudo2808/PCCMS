import React from "react";
import { useParams, useNavigate } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { format } from "date-fns";
import { vi } from "date-fns/locale";
import { ArrowLeft, Stethoscope, FileText, Pill, Activity, CalendarDays } from "lucide-react";
import { Button } from "~/components/atoms";
import { medicalRecordApi } from "~/shared/api/medicalRecordApi";

export const PetMedicalRecordsPage: React.FC = () => {
    const { petId } = useParams<{ petId: string }>();
    const navigate = useNavigate();

    const { data: records, isLoading } = useQuery({
        queryKey: ["owner-medical-records", petId],
        queryFn: () => medicalRecordApi.getOwnerMedicalRecords(petId!),
        enabled: !!petId,
    });

    if (isLoading) {
        return (
            <div className="space-y-6">
                <div className="flex items-center gap-4">
                    <div className="h-10 w-10 animate-pulse rounded-full bg-slate-200"></div>
                    <div className="h-8 w-48 animate-pulse rounded-lg bg-slate-200"></div>
                </div>
                <div className="space-y-8">
                    {[1, 2].map((i) => (
                        <div key={i} className="flex gap-6">
                            <div className="flex flex-col items-center">
                                <div className="h-12 w-12 animate-pulse rounded-full bg-slate-200"></div>
                                <div className="mt-2 h-full w-0.5 bg-slate-100"></div>
                            </div>
                            <div className="h-64 w-full animate-pulse rounded-2xl bg-slate-100"></div>
                        </div>
                    ))}
                </div>
            </div>
        );
    }

    if (!records || records.length === 0) {
        return (
            <div className="flex flex-col items-center justify-center space-y-4 py-24 text-center">
                <div className="flex h-20 w-20 items-center justify-center rounded-full bg-slate-50 text-slate-400">
                    <Stethoscope className="h-10 w-10" />
                </div>
                <div>
                    <h3 className="text-lg font-semibold text-slate-900">Chưa có lịch sử khám</h3>
                    <p className="mt-1 max-w-sm text-sm text-slate-500">
                        Thú cưng của bạn chưa có hồ sơ bệnh án nào được lưu trữ trên hệ thống.
                    </p>
                </div>
                <Button onClick={() => navigate("/owner/pets")} variant="outline" className="mt-4">
                    <ArrowLeft className="mr-2 h-4 w-4" />
                    Quay lại danh sách
                </Button>
            </div>
        );
    }

    return (
        <div className="mx-auto max-w-4xl space-y-10">
            <div className="flex items-center gap-4 border-b border-slate-100 pb-6">
                <Button
                    variant="ghost"
                    onClick={() => navigate("/owner/pets")}
                    className="shrink-0 text-slate-500 hover:text-slate-900 px-2 py-2"
                >
                    <ArrowLeft className="h-5 w-5" />
                </Button>
                <div>
                    <h1 className="text-2xl font-bold tracking-tight text-slate-900">
                        Lịch sử khám bệnh
                    </h1>
                    <p className="text-sm text-slate-500">
                        Danh sách các lần thăm khám và đơn thuốc
                    </p>
                </div>
            </div>

            <div className="relative space-y-12 before:absolute before:inset-0 before:ml-6 before:h-full before:w-0.5 before:-translate-x-px before:bg-gradient-to-b before:from-slate-200 before:via-slate-200 before:to-transparent md:before:ml-8">
                {records.map((record) => (
                    <div key={record.id} className="relative flex items-start gap-6 md:gap-8">
                        {/* Timeline node */}
                        <div className="relative z-10 flex h-12 w-12 shrink-0 items-center justify-center rounded-full border-4 border-white bg-primary-50 text-primary-600 shadow-sm md:h-16 md:w-16">
                            <Stethoscope className="h-5 w-5 md:h-7 md:w-7" />
                        </div>

                        {/* Content */}
                        <div className="flex-1 space-y-4 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm transition-shadow hover:shadow-md md:p-8">
                            {/* Header */}
                            <div className="flex flex-wrap items-center justify-between gap-4 border-b border-slate-100 pb-4">
                                <div>
                                    <h3 className="text-lg font-semibold text-slate-900">
                                        Ngày khám:{" "}
                                        {format(new Date(record.createdAt), "dd MMMM, yyyy", {
                                            locale: vi,
                                        })}
                                    </h3>
                                    <p className="mt-1 text-sm text-slate-500">
                                        Mã bệnh án:{" "}
                                        <span className="font-mono text-slate-700">
                                            {record.recordCode}
                                        </span>{" "}
                                        • Bác sĩ:{" "}
                                        <span className="font-medium text-slate-700">
                                            {record.vetName}
                                        </span>
                                    </p>
                                </div>
                            </div>

                            {/* Vitals */}
                            <div className="grid grid-cols-2 gap-4 rounded-xl bg-slate-50 p-4 sm:grid-cols-4">
                                <div className="flex items-center gap-2">
                                    <Activity className="h-4 w-4 text-slate-400" />
                                    <div>
                                        <p className="text-xs font-medium text-slate-500">Cân nặng</p>
                                        <p className="font-medium text-slate-900">
                                            {record.weightKg ? `${record.weightKg} kg` : "—"}
                                        </p>
                                    </div>
                                </div>
                                <div className="flex items-center gap-2">
                                    <Activity className="h-4 w-4 text-slate-400" />
                                    <div>
                                        <p className="text-xs font-medium text-slate-500">Nhiệt độ</p>
                                        <p className="font-medium text-slate-900">
                                            {record.temperatureC ? `${record.temperatureC} °C` : "—"}
                                        </p>
                                    </div>
                                </div>
                                <div className="flex items-center gap-2">
                                    <Activity className="h-4 w-4 text-slate-400" />
                                    <div>
                                        <p className="text-xs font-medium text-slate-500">Nhịp tim</p>
                                        <p className="font-medium text-slate-900">
                                            {record.heartRateBpm ? `${record.heartRateBpm} bpm` : "—"}
                                        </p>
                                    </div>
                                </div>
                                <div className="flex items-center gap-2">
                                    <Activity className="h-4 w-4 text-slate-400" />
                                    <div>
                                        <p className="text-xs font-medium text-slate-500">Nhịp thở</p>
                                        <p className="font-medium text-slate-900">
                                            {record.respiratoryRateBpm ? `${record.respiratoryRateBpm} l/p` : "—"}
                                        </p>
                                    </div>
                                </div>
                                <div className="flex items-center gap-2">
                                    <Activity className="h-4 w-4 text-slate-400" />
                                    <div>
                                        <p className="text-xs font-medium text-slate-500">Huyết áp</p>
                                        <p className="font-medium text-slate-900">
                                            {record.bloodPressure ? `${record.bloodPressure} mmHg` : "—"}
                                        </p>
                                    </div>
                                </div>
                                <div className="flex items-center gap-2">
                                    <Activity className="h-4 w-4 text-slate-400" />
                                    <div>
                                        <p className="text-xs font-medium text-slate-500">SpO2</p>
                                        <p className="font-medium text-slate-900">
                                            {record.spo2Percent ? `${record.spo2Percent}%` : "—"}
                                        </p>
                                    </div>
                                </div>
                                <div className="flex items-center gap-2">
                                    <Activity className="h-4 w-4 text-slate-400" />
                                    <div>
                                        <p className="text-xs font-medium text-slate-500">Niêm mạc</p>
                                        <p className="font-medium text-slate-900">
                                            {record.mucousMembraneColor || "—"}
                                        </p>
                                    </div>
                                </div>
                                <div className="flex items-center gap-2">
                                    <Activity className="h-4 w-4 text-slate-400" />
                                    <div>
                                        <p className="text-xs font-medium text-slate-500">CRT</p>
                                        <p className="font-medium text-slate-900">
                                            {record.capillaryRefillSeconds ? `${record.capillaryRefillSeconds}s` : "—"}
                                        </p>
                                    </div>
                                </div>
                            </div>

                            {/* Diagnosis */}
                            <div className="space-y-4 pt-2">
                                <div>
                                    <h4 className="flex items-center gap-2 text-sm font-semibold text-slate-900">
                                        <FileText className="h-4 w-4 text-primary-500" /> Chẩn đoán
                                        & Ghi chú
                                    </h4>
                                    <div className="mt-3 space-y-3">
                                        {record.finalDiagnosis && (
                                            <div className="rounded-lg bg-rose-50/50 p-3">
                                                <p className="text-xs font-medium text-rose-800">
                                                    Chẩn đoán chính
                                                </p>
                                                <p className="mt-1 text-sm text-slate-700">
                                                    {record.finalDiagnosis}
                                                </p>
                                            </div>
                                        )}
                                        {record.treatmentNote && (
                                            <div className="rounded-lg bg-blue-50/50 p-3">
                                                <p className="text-xs font-medium text-blue-800">
                                                    Hướng dẫn điều trị
                                                </p>
                                                <p className="mt-1 text-sm text-slate-700">
                                                    {record.treatmentNote}
                                                </p>
                                            </div>
                                        )}
                                    </div>
                                </div>

                                {/* Follow up */}
                                {record.followUpAt && (
                                    <div className="flex items-center gap-2 rounded-lg border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-900">
                                        <CalendarDays className="h-4 w-4 shrink-0 text-amber-600" />
                                        <span>
                                            <strong>Lịch tái khám:</strong>{" "}
                                            {format(
                                                new Date(record.followUpAt),
                                                "dd/MM/yyyy HH:mm",
                                                { locale: vi }
                                            )}
                                        </span>
                                    </div>
                                )}

                                {/* Prescription */}
                                {record.prescription && record.prescription.items.length > 0 && (
                                    <div className="pt-4 border-t border-slate-100">
                                        <h4 className="flex items-center gap-2 text-sm font-semibold text-slate-900">
                                            <Pill className="h-4 w-4 text-emerald-500" /> Đơn thuốc
                                            được kê
                                        </h4>
                                        <div className="mt-3 overflow-hidden rounded-xl border border-slate-200">
                                            <table className="w-full text-left text-sm">
                                                <thead className="bg-slate-50 text-xs text-slate-500">
                                                    <tr>
                                                        <th className="px-4 py-3 font-medium">
                                                            Tên thuốc
                                                        </th>
                                                        <th className="px-4 py-3 font-medium">
                                                            Liều lượng
                                                        </th>
                                                        <th className="px-4 py-3 font-medium">
                                                            Số lượng
                                                        </th>
                                                        <th className="px-4 py-3 font-medium">
                                                            Hướng dẫn
                                                        </th>
                                                    </tr>
                                                </thead>
                                                <tbody className="divide-y divide-slate-100 bg-white">
                                                    {record.prescription.items.map((item) => (
                                                        <tr key={item.id}>
                                                            <td className="px-4 py-3 font-medium text-slate-900">
                                                                {item.medicineName ||
                                                                    "Thuốc không xác định"}
                                                            </td>
                                                            <td className="px-4 py-3 text-slate-600">
                                                                {item.dosage}
                                                            </td>
                                                            <td className="px-4 py-3 text-slate-600">
                                                                {item.quantity}{" "}
                                                                {item.medicineUnit || "Viên"}
                                                            </td>
                                                            <td className="px-4 py-3 text-slate-600">
                                                                {item.instruction || "—"}
                                                            </td>
                                                        </tr>
                                                    ))}
                                                </tbody>
                                            </table>
                                        </div>
                                    </div>
                                )}
                            </div>
                        </div>
                    </div>
                ))}
            </div>
        </div>
    );
};
