import { useQuery } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { Tag } from "~/components/atoms";
import { Card, DataTable, EmptyState } from "~/components/molecules";
import { useAuth } from "~/features/auth/context/AuthContext";
import { medicalRecordApi } from "~/shared/api/medicalRecordApi";
import type { MedicalRecordResponse } from "~/types/medicalRecord";

export function MedicalRecordListPage() {
    const { user } = useAuth();

    const { data: records = [], isLoading } = useQuery({
        queryKey: ["medicalRecords", user?.id],
        queryFn: () => medicalRecordApi.getMedicalRecords(user?.id),
        enabled: !!user?.id,
    });

    if (isLoading) {
        return (
            <div className="flex justify-center p-12">
                <div className="h-8 w-8 animate-spin rounded-full border-b-2 border-indigo-600" />
            </div>
        );
    }

    if (records.length === 0) {
        return (
            <EmptyState
                title="Danh sách bệnh án"
                description="Bạn chưa phụ trách bệnh án nào."
            />
        );
    }

    const rows = records.map((record: MedicalRecordResponse) => [
        record.recordCode,
        <Tag tone={record.recordStatus === "FINALIZED" ? "green" : "amber"}>
            {record.recordStatus === "FINALIZED" ? "Đã chốt" : "Nháp"}
        </Tag>,
        new Date(record.createdAt).toLocaleDateString("vi-VN", {
            day: "2-digit",
            month: "2-digit",
            year: "numeric",
            hour: "2-digit",
            minute: "2-digit",
        }),
        record.petName || record.petId,
        record.preliminaryDiagnosis || "—",
        <Link
            to={`/veterinarian/medical-records/${record.id}`}
            className="text-sm font-semibold text-indigo-600 hover:text-indigo-900"
        >
            Chi tiết
        </Link>,
    ]);

    return (
        <div className="space-y-6">
            <Card title="Danh sách bệnh án của tôi" subtitle="Quản lý và cập nhật thông tin bệnh án do bạn phụ trách.">
                <DataTable
                    columns={[
                        "Mã bệnh án",
                        "Trạng thái",
                        "Ngày tạo",
                        "Thú cưng",
                        "Chẩn đoán sơ bộ",
                        "Hành động",
                    ]}
                    rows={rows}
                />
            </Card>
        </div>
    );
}
