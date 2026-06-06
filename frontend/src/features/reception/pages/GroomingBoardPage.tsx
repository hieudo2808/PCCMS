import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { motion } from "motion/react";
import toast from "react-hot-toast";
import { Tag } from "~/components/atoms";
import { Card, SectionTitle } from "~/components/molecules";
import { appointmentApi } from "~/shared/api/appointmentApi";
import { hasAccessToken } from "~/shared/auth/tokenStorage";
import { useAuth } from "~/features/auth/context/AuthContext";
import type { GroomingBoardCardResponse } from "~/types/appointment";

type BoardColumn = "Chờ làm" | "Đang dùng dịch vụ" | "Hoàn thành";

const COLUMN_STATUS: Record<BoardColumn, string[]> = {
    "Chờ làm": ["PENDING", "CONFIRMED"],
    "Đang dùng dịch vụ": ["IN_SERVICE"],
    "Hoàn thành": ["COMPLETED"],
};

const NEXT_STATUS: Record<string, string> = {
    PENDING: "CONFIRMED",
    CONFIRMED: "IN_SERVICE",
    IN_SERVICE: "COMPLETED",
};

const tones: Record<BoardColumn, "amber" | "blue" | "green"> = {
    "Chờ làm": "amber",
    "Đang dùng dịch vụ": "blue",
    "Hoàn thành": "green",
};

function formatTime(iso: string) {
    return new Date(iso).toLocaleTimeString("vi-VN", { hour: "2-digit", minute: "2-digit" });
}

export function GroomingBoardPage() {
    const queryClient = useQueryClient();
    const { isAuthenticated, user } = useAuth();
    const canFetch = isAuthenticated && hasAccessToken() && Boolean(user);

    const { data: cards = [], isLoading } = useQuery({
        queryKey: ["appointments", "grooming-board"],
        queryFn: () => appointmentApi.listGroomingBoard(),
        enabled: canFetch,
        refetchInterval: 30_000,
    });

    const updateMutation = useMutation({
        mutationFn: ({
            ticketId,
            status,
        }: {
            ticketId: string;
            status: "PENDING" | "CONFIRMED" | "IN_SERVICE" | "COMPLETED" | "CANCELLED";
        }) => appointmentApi.updateGroomingStatus(ticketId, status),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ["appointments", "grooming-board"] });
        },
        onError: () => toast.error("Không thể cập nhật trạng thái"),
    });

    const grouped = (Object.keys(COLUMN_STATUS) as BoardColumn[]).reduce(
        (acc, column) => {
            acc[column] = cards.filter((c: GroomingBoardCardResponse) =>
                COLUMN_STATUS[column].includes(c.statusCode)
            );
            return acc;
        },
        {} as Record<BoardColumn, GroomingBoardCardResponse[]>
    );

    return (
        <div className="space-y-6">
            <SectionTitle title="Bảng trạng thái dịch vụ làm đẹp" />
            {isLoading ? (
                <p className="text-center text-sm text-slate-500">Đang tải...</p>
            ) : (
                <div className="grid gap-4 lg:grid-cols-3">
                    {(Object.keys(COLUMN_STATUS) as BoardColumn[]).map((title) => {
                        const columnCards = grouped[title];
                        return (
                            <Card
                                key={title}
                                title={title}
                                right={<Tag tone={tones[title]}>{columnCards.length} thẻ</Tag>}
                            >
                                <div className="space-y-3">
                                    {columnCards.length === 0 ? (
                                        <p className="text-sm text-slate-400">Chưa có phiếu</p>
                                    ) : (
                                        columnCards.map((card) => (
                                            <motion.div
                                                key={card.ticketId}
                                                whileHover={{ y: -2 }}
                                                className="rounded-3xl border border-slate-200 bg-slate-50 p-4 transition hover:shadow-sm"
                                            >
                                                <div className="flex items-start justify-between gap-3">
                                                    <div>
                                                        <p className="font-medium">
                                                            {card.petName} • {card.serviceName}
                                                        </p>
                                                        <p className="mt-1 text-sm text-slate-500">
                                                            {formatTime(card.scheduledStartAt)}
                                                            {card.stationName
                                                                ? ` • ${card.stationName}`
                                                                : ""}
                                                        </p>
                                                    </div>
                                                    <Tag tone={tones[title]}>{card.statusLabel}</Tag>
                                                </div>
                                                {NEXT_STATUS[card.statusCode] && (
                                                    <button
                                                        type="button"
                                                        className="mt-3 text-sm font-medium text-emerald-600 hover:underline"
                                                        onClick={() =>
                                                            updateMutation.mutate({
                                                                ticketId: card.ticketId,
                                                                status: NEXT_STATUS[
                                                                    card.statusCode
                                                                ] as "CONFIRMED" | "IN_SERVICE" | "COMPLETED",
                                                            })
                                                        }
                                                    >
                                                        Chuyển tiếp →
                                                    </button>
                                                )}
                                            </motion.div>
                                        ))
                                    )}
                                </div>
                            </Card>
                        );
                    })}
                </div>
            )}
        </div>
    );
}
