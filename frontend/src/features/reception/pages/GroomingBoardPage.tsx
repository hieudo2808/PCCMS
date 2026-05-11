import { motion } from "motion/react";
import { Tag } from "~/components/atoms";
import { Card, SectionTitle } from "~/components/molecules";

const columns: Record<string, string[]> = {
    "Chờ làm": ["Milu • Tắm + Sấy", "Luna • Cắt móng"],
    "Đang dùng dịch vụ": ["Mít • Spa premium"],
    "Hoàn thành": ["Bơ • Cắt tỉa lông"],
};

const tones: Record<string, "amber" | "blue" | "green"> = {
    "Chờ làm": "amber",
    "Đang dùng dịch vụ": "blue",
    "Hoàn thành": "green",
};

export function GroomingBoardPage() {
    return (
        <div className="space-y-6">
            <SectionTitle title="Bảng trạng thái dịch vụ làm đẹp" />
            <div className="grid gap-4 lg:grid-cols-3">
                {Object.entries(columns).map(([title, cards]) => (
                    <Card
                        key={title}
                        title={title}
                        right={<Tag tone={tones[title]}>{cards.length} thẻ</Tag>}
                    >
                        <div className="space-y-3">
                            {cards.map((card) => (
                                <motion.div
                                    key={card}
                                    whileHover={{ y: -2 }}
                                    className="cursor-grab rounded-3xl border border-slate-200 bg-slate-50 p-4 transition hover:shadow-sm"
                                >
                                    <div className="flex items-start justify-between gap-3">
                                        <div>
                                            <p className="font-medium">{card}</p>
                                            <p className="mt-1 text-sm text-slate-500">Phiếu DV • cập nhật 10:35</p>
                                        </div>
                                        <Tag tone={tones[title]}>{title}</Tag>
                                    </div>
                                </motion.div>
                            ))}
                        </div>
                    </Card>
                ))}
            </div>
        </div>
    );
}
