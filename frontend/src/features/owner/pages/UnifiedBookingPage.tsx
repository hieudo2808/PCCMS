import { useState } from "react";
import { Stepper } from "~/components/molecules/Stepper";
import { Button, Input, Select, Textarea } from "~/components/atoms";
import { Card, SummaryRow } from "~/components/molecules";

const bookingSteps = [{ label: "Chọn Dịch Vụ" }, { label: "Lịch Phục Vụ" }, { label: "Xác Nhận" }];

export function UnifiedBookingPage() {
    const [step, setStep] = useState(0);

    const nextStep = () => {
        if (step < 2) setStep(step + 1);
    };

    const prevStep = () => {
        if (step > 0) setStep(step - 1);
    };

    return (
        <div className="grid gap-6 lg:grid-cols-[1fr_320px]">
            {/* Vùng trái: Form làm việc chính */}
            <div className="space-y-6">
                <div className="mb-6 rounded-2xl bg-white p-6 shadow-sm border border-border-main">
                    <Stepper steps={bookingSteps} currentStep={step} />
                </div>

                <Card className="min-h-[400px]">
                    {step === 0 && (
                        <div className="space-y-5 animate-in fade-in slide-in-from-bottom-2">
                            <h2 className="text-xl font-semibold">Dịch vụ chăm sóc</h2>

                            <Select
                                label="Loại Dịch Vụ Cần Đặt"
                                options={[
                                    "Chọn loại dịch vụ...",
                                    "Khám Bệnh Tổng Quát",
                                    "Spa / Làm Đẹp",
                                    "Lưu Trú (Boarding)",
                                ]}
                            />

                            <Select
                                label="Chi Tiết Dịch Vụ (Tuỳ Theo Mục Trên)"
                                options={[
                                    "Chọn chi tiết...",
                                    "Bác sĩ thú y tự gán",
                                    "Cắt tỉa lông cơ bản",
                                    "Chuồng siêu VIP",
                                ]}
                                helperText="Danh sách này phụ thuộc vào loại dịch vụ đã chọn."
                            />
                        </div>
                    )}

                    {step === 1 && (
                        <div className="space-y-5 animate-in fade-in slide-in-from-bottom-2 w-full">
                            <h2 className="text-xl font-semibold">Ngày giờ & Thú cưng</h2>

                            <div className="grid gap-4 md:grid-cols-2">
                                <Input label="Ngày Sử Dụng" type="date" required />
                                <Select
                                    label="Khung Giờ"
                                    options={[
                                        "Chọn giờ...",
                                        "09:00 - 10:00",
                                        "14:00 - 15:00",
                                        "16:00 - 17:00",
                                    ]}
                                    required
                                />
                            </div>

                            <Select
                                label="Chọn Thú Cưng Đăng Ký"
                                options={["Milu (Poodle)", "Bơ (Corgi)", "Mít (Mèo ALN)"]}
                                required
                            />

                            <Textarea
                                label="Ghi Chú Đặc Biệt"
                                placeholder="Dị ứng, tiền sử bệnh, lời dặn bác sĩ..."
                                rows={4}
                            />
                        </div>
                    )}

                    {step === 2 && (
                        <div className="space-y-5 animate-in fade-in slide-in-from-bottom-2">
                            <h2 className="text-xl font-semibold">Xác Nhận Đơn Hàng</h2>
                            <p className="text-sm text-text-muted">
                                Kiểm tra kỹ thông tin bên lề trước khi gửi yêu cầu.
                            </p>

                            <div className="rounded-xl border border-warning-200 bg-warning-50 p-4">
                                <p className="text-sm font-semibold text-warning-700">
                                    Lưu ý trước khi đặt hẹn
                                </p>
                                <p className="mt-1 text-[13px] text-warning-600">
                                    Nếu bạn muốn hủy ngang, cần nhắn trước ít nhất 2 giờ trên hệ
                                    thống để chúng tôi sắp xếp lại.
                                </p>
                            </div>
                        </div>
                    )}
                </Card>

                {/* Controller Nav Bar dưới form */}
                <div className="flex items-center justify-between border-t border-slate-200 pt-5">
                    <Button variant="outline" onClick={prevStep} disabled={step === 0}>
                        Quay lại
                    </Button>
                    <Button variant="primary" onClick={nextStep} disabled={step === 2}>
                        Tiếp tục
                    </Button>
                    {step === 2 && <Button variant="primary">Hoàn tất đặt lịch</Button>}
                </div>
            </div>

            {/* Vùng phải: Summary Drawer */}
            <div className="sticky top-24 h-fit space-y-4">
                <Card title="Tóm tắt Lịch Dự Kiến">
                    <div className="space-y-3 text-sm">
                        <SummaryRow label="Dịch vụ" value="Chưa chọn" />
                        <SummaryRow label="Thú cưng" value="Chưa chọn" />
                        <SummaryRow label="Ngày sử dụng" value="---" />
                        <SummaryRow label="Giờ" value="---" />
                        <div className="h-px bg-slate-200 my-2" />
                        <SummaryRow label="Chi phí dự tính" value="Theo giá Menu" />
                    </div>
                </Card>
            </div>
        </div>
    );
}
