import { PawPrint, CalendarDays, Building2, CreditCard } from "lucide-react";
import { Card, MiniGridStats, Notice } from "~/components/molecules";

export function OwnerDashboard() {
    return (
        <div className="space-y-6">
            <MiniGridStats
                items={[
                    {
                        label: "Thú cưng của tôi",
                        value: "3",
                        hint: "2 hồ sơ đang hoạt động",
                        icon: PawPrint,
                    },
                    {
                        label: "Lịch hẹn sắp tới",
                        value: "2",
                        hint: "1 lịch trong 24 giờ",
                        icon: CalendarDays,
                    },
                    {
                        label: "Đang lưu trú",
                        value: "1",
                        hint: "Milu ở phòng C12",
                        icon: Building2,
                    },
                    {
                        label: "Hóa đơn tháng này",
                        value: "4",
                        hint: "1 chưa thanh toán",
                        icon: CreditCard,
                    },
                ]}
            />
            <div className="grid gap-6 xl:grid-cols-[1.2fr_0.8fr]">
                <Card title="Đặt dịch vụ nhanh">
                    <div className="grid gap-4 md:grid-cols-1">
                        <div
                            onClick={() => (window.location.href = "/owner/grooming/book")}
                            className="cursor-pointer rounded-3xl border border-slate-200 p-6 transition hover:border-primary-300 hover:shadow-md flex items-center gap-4 bg-primary-50"
                        >
                            <div className="inline-flex rounded-2xl bg-white p-4 text-primary-700 shadow-sm">
                                <CalendarDays className="h-6 w-6" />
                            </div>
                            <div>
                                <h4 className="text-lg font-bold text-slate-900">
                                    Đăng ký dịch vụ làm đẹp
                                </h4>
                                <p className="mt-1 text-sm text-slate-500">
                                    Chọn thú cưng, gói tắm sấy/cắt tỉa và giờ hẹn phù hợp.
                                </p>
                            </div>
                        </div>
                    </div>
                </Card>
                <Card title="Thông báo gần đây">
                    <div className="space-y-3">
                        <Notice
                            tone="green"
                            title="Lịch khám đã xác nhận"
                            text="Milu - 09:00, 24/05/2026 với BS. An"
                        />
                        <Notice
                            tone="blue"
                            title="Nhật ký lưu trú mới"
                            text="Bé ăn tốt, vệ sinh bình thường, đã cập nhật 2 ảnh."
                        />
                        <Notice
                            tone="amber"
                            title="Hóa đơn chờ thanh toán"
                            text="HD-2026-041 cần thanh toán trước khi đón bé."
                        />
                    </div>
                </Card>
            </div>
        </div>
    );
}
