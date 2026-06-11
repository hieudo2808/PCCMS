package com.astral.express.pccms.appointment.service;

import com.astral.express.pccms.appointment.dto.response.*;
import com.astral.express.pccms.appointment.entity.*;
import com.astral.express.pccms.boarding.entity.BoardingBooking;
import com.astral.express.pccms.boarding.entity.BoardingStatus;
import com.astral.express.pccms.pet.entity.Pets;
import com.astral.express.pccms.room.entity.RoomType;
import com.astral.express.pccms.user.entity.Users;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalTime;

@Component
public class AppointmentResponseAssembler {

    public AppointmentResponse toResponse(Appointment appointment, Integer queueNumber) {
        ServiceOrder order = appointment.getServiceOrder();
        Users owner = order.getOwner();
        Pets pet = order.getPet();
        Users vet = appointment.getAssignedStaff();

        return new AppointmentResponse(
                appointment.getId(),
                order.getOrderCode(),
                appointment.getAppointmentType(),
                order.getService().getName(),
                appointment.getScheduledStartAt(),
                appointment.getScheduledEndAt(),
                owner.getFullName(),
                owner.getPhone(),
                pet.getId(),
                pet.getName(),
                vet != null ? vet.getId() : null,
                vet != null ? formatVetName(vet.getFullName()) : null,
                appointment.getStatusCode(),
                toStatusLabel(appointment.getStatusCode()),
                appointment.getSymptomText(),
                appointment.getOwnerNote(),
                queueNumber
        );
    }

    public GroomingBoardCardResponse toGroomingBoardCard(GroomingTicket ticket) {
        Appointment appointment = ticket.getAppointment();
        ServiceOrder order = appointment.getServiceOrder();
        return new GroomingBoardCardResponse(
                ticket.getId(),
                appointment.getId(),
                order.getPet().getName(),
                order.getService().getName(),
                appointment.getScheduledStartAt(),
                ticket.getStatusCode(),
                toGroomingStatusLabel(ticket.getStatusCode()),
                ticket.getStation() != null ? ticket.getStation().getName() : null
        );
    }

    public BoardingBookingResponse toBoardingResponse(BoardingBooking booking) {
        return new BoardingBookingResponse(
                booking.getId(),
                booking.getBookingCode(),
                booking.getPet().getId(),
                booking.getPet().getName(),
                booking.getRequestedRoomType().getName(),
                booking.getExpectedCheckinAt(),
                booking.getExpectedCheckoutAt(),
                booking.getEstimatedPriceVnd() != null ? BigDecimal.valueOf(booking.getEstimatedPriceVnd()) : BigDecimal.ZERO,
                booking.getStatusCode(),
                toBoardingStatusLabel(booking.getStatusCode()),
                booking.getSpecialCareRequest()
        );
    }

    public RoomTypeOptionResponse toRoomTypeOptionResponse(RoomType rt) {
        return new RoomTypeOptionResponse(
                rt.getId(), 
                rt.getCode(), 
                rt.getName(), 
                rt.getBaseDailyPriceVnd() != null ? BigDecimal.valueOf(rt.getBaseDailyPriceVnd()) : BigDecimal.ZERO
        );
    }

    public ServiceCatalogOptionResponse toServiceCatalogOptionResponse(ServiceCatalog s) {
        return new ServiceCatalogOptionResponse(
                s.getId(), 
                s.getServiceCode(), 
                s.getName(),
                s.getCategoryCode(), 
                s.getBasePriceVnd(), 
                s.getDurationMinutes()
        );
    }

    public VetOptionResponse toVetOptionResponse(Users vet, boolean available) {
        return new VetOptionResponse(
                vet.getId(), 
                formatVetName(vet.getFullName()), 
                available
        );
    }

    public TimeSlotResponse toTimeSlotResponse(LocalTime start, LocalTime end, boolean available) {
        return new TimeSlotResponse(
                start, 
                end, 
                formatSlotLabel(start, end), 
                available
        );
    }

    public String formatVetName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return "Bác sĩ";
        }
        return fullName.startsWith("BS") ? fullName : "BS. " + fullName;
    }

    public String formatSlotLabel(LocalTime start, LocalTime end) {
        return String.format("%02d:%02d - %02d:%02d", start.getHour(), start.getMinute(), end.getHour(), end.getMinute());
    }

    private String toGroomingStatusLabel(GroomingStatus status) {
        return switch (status) {
            case PENDING -> "Chờ làm";
            case CONFIRMED -> "Đã xác nhận";
            case IN_SERVICE -> "Đang dùng dịch vụ";
            case COMPLETED -> "Hoàn thành";
            case CANCELLED -> "Đã hủy";
        };
    }

    private String toBoardingStatusLabel(BoardingStatus status) {
        return switch (status) {
            case RESERVED -> "Đã đặt phòng";
            case CHECKED_IN -> "Đã nhận phòng";
            case IN_STAY -> "Đang lưu trú";
            case CHECKED_OUT -> "Đã trả phòng";
            case CANCELLED -> "Đã hủy";
        };
    }

    private String toStatusLabel(AppointmentStatus status) {
        return switch (status) {
            case PENDING -> "Chờ tiếp nhận";
            case CONFIRMED -> "Đã xác nhận";
            case CHECKED_IN -> "Đang chờ khám";
            case IN_PROGRESS -> "Đang khám";
            case COMPLETED -> "Hoàn thành";
            case CANCELLED -> "Đã hủy";
        };
    }
}
