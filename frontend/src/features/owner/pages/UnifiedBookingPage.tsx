import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useNavigate } from "react-router-dom";
import {
    BookingServicePicker,
    bookingServiceLabel,
    type BookingServiceType,
} from "~/features/owner/components/BookingServicePicker";

import toast from "react-hot-toast";


import { Stepper } from "~/components/molecules/Stepper";

import { Button, Input, Select, Textarea } from "~/components/atoms";

import { Card, SummaryRow, TimeSlot } from "~/components/molecules";

import { PetSelect, PetProfileSummary } from "~/shared/components/pet";

import { usePetProfile } from "~/shared/hooks/usePetProfile";

import { appointmentApi } from "~/shared/api/appointmentApi";

import { hasAccessToken } from "~/shared/auth/tokenStorage";

import { useAuth } from "~/features/auth/context/AuthContext";

import type { TimeSlotResponse } from "~/types/appointment";

import { cx } from "~/utils/cx";



function todayIso() {
    return new Date().toISOString().slice(0, 10);
}



export function UnifiedBookingPage() {
    const navigate = useNavigate();
    const queryClient = useQueryClient();
    const { isAuthenticated, user } = useAuth();
    const canFetch = isAuthenticated && hasAccessToken() && Boolean(user);

    const [step, setStep] = useState(0);
    const [serviceType, setServiceType] = useState<BookingServiceType>("medical");

    const [selectedPetId, setSelectedPetId] = useState("");

    const [appointmentDate, setAppointmentDate] = useState(todayIso());

    const [slotStart, setSlotStart] = useState("");

    const [requestedVetId, setRequestedVetId] = useState("");

    const [serviceCode, setServiceCode] = useState("");

    const [roomTypeId, setRoomTypeId] = useState("");

    const [checkinDate, setCheckinDate] = useState(todayIso());

    const [checkoutDate, setCheckoutDate] = useState("");

    const [symptomText, setSymptomText] = useState("");

    const [ownerNote, setOwnerNote] = useState("");

    const [specialCare, setSpecialCare] = useState("");



    const bookingSteps = [

        { label: "Chọn dịch vụ" },

        { label: "Thông tin đặt lịch" },

        { label: "Xác nhận" },

    ];



    const { data: selectedPet } = usePetProfile(selectedPetId || undefined);



    const { data: groomingServices = [] } = useQuery({

        queryKey: ["appointments", "services", "GROOMING"],

        queryFn: () => appointmentApi.listServices("GROOMING"),

        enabled: canFetch && serviceType === "grooming",

    });



    const { data: roomTypes = [] } = useQuery({

        queryKey: ["appointments", "room-types"],

        queryFn: () => appointmentApi.listRoomTypes(),

        enabled: canFetch && serviceType === "boarding",

    });



    const showSlots = serviceType === "medical" || serviceType === "grooming";



    const {

        data: slots = [],

        isLoading: slotsLoading,

        isError: slotsError,

    } = useQuery({

        queryKey: ["appointments", "slots", appointmentDate, requestedVetId],

        queryFn: () =>

            appointmentApi.getAvailableSlots(appointmentDate, requestedVetId || undefined),

        enabled: canFetch && Boolean(appointmentDate) && showSlots,

        retry: 1,

    });



    const { data: vetsOnDuty = [] } = useQuery({

        queryKey: ["appointments", "vets-on-duty", appointmentDate],

        queryFn: () => appointmentApi.listVetsOnDuty(appointmentDate),

        enabled: canFetch && Boolean(appointmentDate) && step >= 1 && serviceType === "medical",

    });



    const { data: vetsForSlot = [] } = useQuery({

        queryKey: ["appointments", "vets", appointmentDate, slotStart],

        queryFn: () => appointmentApi.listAvailableVets(appointmentDate, slotStart),

        enabled:

            canFetch &&

            Boolean(appointmentDate) &&

            Boolean(slotStart) &&

            step >= 1 &&

            serviceType === "medical",

    });



    const vets = slotStart ? vetsForSlot : vetsOnDuty;



    const { data: availability } = useQuery({

        queryKey: ["appointments", "availability", appointmentDate, slotStart],

        queryFn: () =>

            appointmentApi.getAvailabilitySummary(appointmentDate, slotStart || undefined),

        enabled: canFetch && Boolean(appointmentDate) && step >= 1 && serviceType === "medical",

    });



    const availableSlotCount = slots.filter((s: TimeSlotResponse) => s.available).length;



    const createMutation = useMutation({

        mutationFn: async () => {

            if (serviceType === "medical") {

                return appointmentApi.createMedicalAppointment({

                    petId: selectedPetId,

                    appointmentDate,

                    slotStart,

                    requestedVetId: requestedVetId || undefined,

                    symptomText: symptomText.trim(),

                    ownerNote: ownerNote.trim() || undefined,

                });

            }

            if (serviceType === "grooming") {

                return appointmentApi.createGroomingAppointment({

                    petId: selectedPetId,

                    serviceCode,

                    appointmentDate,

                    slotStart,

                    ownerNote: ownerNote.trim() || undefined,

                });

            }

            return appointmentApi.createBoardingBooking({

                petId: selectedPetId,

                roomTypeId,

                checkinDate,

                checkoutDate,

                specialCareRequest: specialCare.trim() || undefined,

            });

        },

        onSuccess: () => {

            toast.success("Đặt lịch thành công");

            queryClient.invalidateQueries({ queryKey: ["appointments"] });

            navigate("/owner/appointments");

        },

        onError: () => {

            toast.error("Không thể đặt lịch. Vui lòng kiểm tra lại thông tin.");

        },

    });



    const normalizeTime = (value: string) => value?.slice(0, 5) ?? "";



    const selectedSlot = slots.find(

        (s: TimeSlotResponse) => normalizeTime(s.startTime) === normalizeTime(slotStart)

    );



    const selectedGroomingService = groomingServices.find((s) => s.serviceCode === serviceCode);

    const selectedRoomType = roomTypes.find((r) => r.id === roomTypeId);



    const canProceedStep1 =

        Boolean(selectedPetId) &&

        (serviceType === "medical"

            ? Boolean(appointmentDate) &&

              Boolean(slotStart) &&

              symptomText.trim().length > 0 &&

              symptomText.length <= 500

            : serviceType === "grooming"

              ? Boolean(appointmentDate) && Boolean(slotStart) && Boolean(serviceCode)

              : Boolean(checkinDate) &&

                Boolean(checkoutDate) &&

                Boolean(roomTypeId) &&

                checkoutDate > checkinDate);



    const nextStep = () => {

        if (step === 0 && !serviceType) {

            toast.error("Chọn loại dịch vụ");

            return;

        }

        if (step === 1 && !canProceedStep1) {

            toast.error("Vui lòng điền đủ thông tin bắt buộc");

            return;

        }

        if (step < 2) setStep(step + 1);

    };



    const prevStep = () => {

        if (step > 0) setStep(step - 1);

    };



    const handleSubmit = () => {

        if (!canProceedStep1) return;

        createMutation.mutate();

    };



    return (

        <div className="grid gap-6 lg:grid-cols-[1fr_320px]">

            <div className="space-y-6">

                <div className="mb-6 rounded-2xl border border-border-main bg-white p-6 shadow-sm">

                    <Stepper steps={bookingSteps} currentStep={step} />

                </div>



                <Card className="min-h-[400px]">

                    {step === 0 && (
                        <div className="animate-in fade-in slide-in-from-bottom-2 space-y-5">
                            <h2 className="text-xl font-semibold">Chọn loại dịch vụ</h2>
                            <p className="text-sm text-text-muted">
                                Chọn khám bệnh, spa hoặc lưu trú — mỗi dịch vụ có biểu mẫu riêng ở bước
                                tiếp theo.
                            </p>
                            <BookingServicePicker
                                value={serviceType}
                                onChange={setServiceType}
                            />
                        </div>
                    )}



                    {step === 1 && (

                        <div className="animate-in fade-in slide-in-from-bottom-2 w-full space-y-5">

                            <h2 className="text-xl font-semibold">

                                {serviceType === "boarding" ? "Thông tin lưu trú" : "Ngày giờ & Thú cưng"}

                            </h2>



                            {serviceType === "medical" && availability && (

                                <div className="rounded-xl border border-slate-200 bg-slate-50 p-4 text-sm text-slate-700">

                                    <p className="font-medium text-slate-900">Tình trạng phòng khám</p>

                                    <ul className="mt-2 space-y-1">

                                        <li>• {availability.totalExamRooms} phòng khám đang hoạt động</li>

                                        <li>• {availability.vetsOnDuty} bác sĩ trực</li>

                                        <li>

                                            • {availability.availableSlots}/{availability.totalSlots} khung giờ

                                            còn trống

                                        </li>

                                    </ul>

                                </div>

                            )}



                            {serviceType === "grooming" && (

                                <Select

                                    label="Gói dịch vụ spa"

                                    required

                                    value={serviceCode}

                                    onChange={(e) => setServiceCode(e.target.value)}

                                    options={[

                                        { value: "", label: "Chọn gói dịch vụ" },

                                        ...groomingServices.map((s) => ({

                                            value: s.serviceCode,

                                            label: `${s.name} — ${Number(s.basePriceVnd).toLocaleString("vi-VN")}đ`,

                                        })),

                                    ]}

                                />

                            )}



                            {serviceType === "boarding" ? (

                                <div className="grid gap-4 md:grid-cols-2">

                                    <Input

                                        label="Ngày nhận phòng"

                                        type="date"

                                        required

                                        min={todayIso()}

                                        value={checkinDate}

                                        onChange={(e) => setCheckinDate(e.target.value)}

                                    />

                                    <Input

                                        label="Ngày trả phòng"

                                        type="date"

                                        required

                                        min={checkinDate || todayIso()}

                                        value={checkoutDate}

                                        onChange={(e) => setCheckoutDate(e.target.value)}

                                    />

                                    <div className="md:col-span-2">

                                        <Select

                                            label="Loại phòng"

                                            required

                                            value={roomTypeId}

                                            onChange={(e) => setRoomTypeId(e.target.value)}

                                            options={[

                                                { value: "", label: "Chọn loại phòng" },

                                                ...roomTypes.map((r) => ({

                                                    value: r.id,

                                                    label: `${r.name} — ${Number(r.baseDailyPriceVnd).toLocaleString("vi-VN")}đ/ngày`,

                                                })),

                                            ]}

                                        />

                                    </div>

                                </div>

                            ) : (

                                <>

                                    <div className="grid gap-4 md:grid-cols-2">

                                        <Input

                                            label={serviceType === "grooming" ? "Ngày hẹn" : "Ngày khám"}

                                            type="date"

                                            required

                                            min={todayIso()}

                                            value={appointmentDate}

                                            onChange={(e) => {

                                                setAppointmentDate(e.target.value);

                                                setSlotStart("");

                                                setRequestedVetId("");

                                            }}

                                        />

                                        {serviceType === "medical" && (

                                            <Select

                                                label="Bác sĩ thú y (tuỳ chọn)"

                                                value={requestedVetId}

                                                onChange={(e) => setRequestedVetId(e.target.value)}

                                                options={[

                                                    { value: "", label: "Hệ thống tự gán" },

                                                    ...vets.map((v) => ({

                                                        value: v.id,

                                                        label: `${v.fullName}${v.available ? "" : " (bận)"}`,

                                                    })),

                                                ]}

                                            />

                                        )}

                                    </div>



                                    <div>

                                        <div className="mb-2 flex flex-wrap items-center justify-between gap-2">

                                            <p className="text-[13px] font-medium text-slate-700">

                                                Khung giờ <span className="text-error-500">*</span>

                                            </p>

                                            <p className="text-xs text-slate-500">

                                                Còn trống ({availableSlotCount})

                                            </p>

                                        </div>

                                        {slotsLoading ? (

                                            <p className="text-sm text-slate-500">Đang tải khung giờ...</p>

                                        ) : slotsError ? (

                                            <p className="text-sm text-red-600">Không tải được khung giờ.</p>

                                        ) : (

                                            <div className="grid grid-cols-2 gap-2 sm:grid-cols-3 md:grid-cols-4">

                                                {slots.map((slot: TimeSlotResponse) => {

                                                    const isSelected =

                                                        normalizeTime(slot.startTime) ===

                                                        normalizeTime(slotStart);

                                                    return (

                                                        <button

                                                            key={slot.label}

                                                            type="button"

                                                            disabled={!slot.available}

                                                            onClick={() => setSlotStart(slot.startTime)}

                                                            className={cx(

                                                                "text-left rounded-2xl transition",

                                                                isSelected && "ring-2 ring-emerald-500"

                                                            )}

                                                        >

                                                            <TimeSlot

                                                                text={slot.label}

                                                                available={slot.available}

                                                            />

                                                        </button>

                                                    );

                                                })}

                                            </div>

                                        )}

                                    </div>

                                </>

                            )}



                            <PetSelect

                                label="Thú cưng"

                                value={selectedPetId}

                                onChange={setSelectedPetId}

                                required

                            />



                            {selectedPetId && <PetProfileSummary petId={selectedPetId} />}



                            {serviceType === "medical" && (

                                <Textarea

                                    label="Triệu chứng ban đầu"

                                    required

                                    maxLength={500}

                                    value={symptomText}

                                    onChange={(e) => setSymptomText(e.target.value)}

                                    placeholder="Mô tả biểu hiện lạ để bác sĩ chuẩn bị..."

                                    rows={3}

                                />

                            )}



                            {serviceType === "boarding" ? (

                                <Textarea

                                    label="Yêu cầu chăm sóc đặc biệt"

                                    maxLength={500}

                                    value={specialCare}

                                    onChange={(e) => setSpecialCare(e.target.value)}

                                    placeholder="Chế độ ăn, thuốc, thói quen..."

                                    rows={3}

                                />

                            ) : (

                                <Textarea

                                    label="Ghi chú thêm"

                                    maxLength={255}

                                    value={ownerNote}

                                    onChange={(e) => setOwnerNote(e.target.value)}

                                    rows={2}

                                />

                            )}

                        </div>

                    )}



                    {step === 2 && (

                        <div className="animate-in fade-in slide-in-from-bottom-2 space-y-5">

                            <h2 className="text-xl font-semibold">Xác nhận đặt lịch</h2>

                            {selectedPetId && <PetProfileSummary petId={selectedPetId} />}

                            <div className="rounded-xl border border-warning-200 bg-warning-50 p-4">

                                <p className="text-sm font-semibold text-warning-700">Lưu ý</p>

                                <p className="mt-1 text-[13px] text-warning-600">

                                    {serviceType === "boarding"

                                        ? "Đặt phòng sẽ xuất hiện trong mục Lịch hẹn của tôi."

                                        : "Lịch hẹn xuất hiện tại quầy lễ tân với trạng thái Chờ tiếp nhận."}

                                </p>

                            </div>

                        </div>

                    )}

                </Card>



                <div className="flex items-center justify-between border-t border-slate-200 pt-5">

                    <Button variant="outline" onClick={prevStep} disabled={step === 0}>

                        Quay lại

                    </Button>

                    {step < 2 ? (

                        <Button variant="primary" onClick={nextStep}>

                            Tiếp tục

                        </Button>

                    ) : (

                        <Button

                            variant="primary"

                            onClick={handleSubmit}

                            disabled={createMutation.isPending}

                        >

                            {createMutation.isPending ? "Đang gửi..." : "Xác nhận đặt lịch"}

                        </Button>

                    )}

                </div>

            </div>



            <div className="sticky top-24 h-fit space-y-4">

                <Card title="Tóm tắt lịch hẹn">

                    <div className="space-y-3 text-sm">

                        <SummaryRow label="Dịch vụ" value={bookingServiceLabel(serviceType)} />

                        <SummaryRow

                            label="Thú cưng"

                            value={selectedPet ? selectedPet.name : "Chưa chọn"}

                        />

                        {serviceType === "boarding" ? (

                            <>

                                <SummaryRow

                                    label="Nhận phòng"

                                    value={

                                        checkinDate

                                            ? new Date(checkinDate).toLocaleDateString("vi-VN")

                                            : "---"

                                    }

                                />

                                <SummaryRow

                                    label="Trả phòng"

                                    value={

                                        checkoutDate

                                            ? new Date(checkoutDate).toLocaleDateString("vi-VN")

                                            : "---"

                                    }

                                />

                                <SummaryRow

                                    label="Loại phòng"

                                    value={selectedRoomType?.name ?? "---"}

                                />

                            </>

                        ) : (

                            <>

                                <SummaryRow

                                    label="Ngày"

                                    value={

                                        appointmentDate

                                            ? new Date(appointmentDate).toLocaleDateString("vi-VN")

                                            : "---"

                                    }

                                />

                                <SummaryRow label="Giờ" value={selectedSlot?.label ?? "---"} />

                                {serviceType === "grooming" && (

                                    <SummaryRow

                                        label="Gói spa"

                                        value={selectedGroomingService?.name ?? "---"}

                                    />

                                )}

                            </>

                        )}

                    </div>

                </Card>

            </div>

        </div>

    );

}


