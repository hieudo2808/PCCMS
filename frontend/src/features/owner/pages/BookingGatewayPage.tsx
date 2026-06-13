import { useNavigate } from "react-router-dom";
import { useState } from "react";
import { Card } from "~/components/molecules";
import { Button } from "~/components/atoms";
import { BookingServicePicker, type BookingServiceType } from "../components/BookingServicePicker";

export function BookingGatewayPage() {
    const navigate = useNavigate();
    const [serviceType, setServiceType] = useState<BookingServiceType>("medical");

    const handleContinue = () => {
        if (serviceType === "medical") {
            navigate("/owner/medical/book");
        } else if (serviceType === "grooming") {
            navigate("/owner/grooming/book");
        } else if (serviceType === "boarding") {
            navigate("/owner/boarding/book");
        }
    };

    return (
        <div className="mx-auto max-w-4xl space-y-6">
            <h1 className="text-2xl font-bold tracking-tight text-slate-900">Đặt lịch dịch vụ</h1>
            <p className="text-slate-600">
                Lựa chọn loại dịch vụ mà bạn muốn đặt lịch cho thú cưng của mình.
            </p>
            
            <Card>
                <div className="space-y-6">
                    <BookingServicePicker
                        value={serviceType}
                        onChange={setServiceType}
                    />
                    <div className="flex justify-end">
                        <Button onClick={handleContinue}>Tiếp tục</Button>
                    </div>
                </div>
            </Card>
        </div>
    );
}
