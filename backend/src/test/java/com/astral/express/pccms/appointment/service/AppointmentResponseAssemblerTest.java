package com.astral.express.pccms.appointment.service;

import com.astral.express.pccms.appointment.dto.response.AppointmentResponse;
import com.astral.express.pccms.appointment.dto.response.GroomingBoardCardResponse;
import com.astral.express.pccms.appointment.dto.response.TimeSlotResponse;
import com.astral.express.pccms.appointment.entity.Appointment;
import com.astral.express.pccms.appointment.entity.AppointmentStatus;
import com.astral.express.pccms.appointment.entity.GroomingStatus;
import com.astral.express.pccms.appointment.entity.GroomingTicket;
import com.astral.express.pccms.appointment.entity.ServiceOrder;
import com.astral.express.pccms.appointment.dto.response.BoardingBookingResponse;
import com.astral.express.pccms.boarding.entity.BoardingBooking;
import com.astral.express.pccms.boarding.entity.BoardingStatus;
import com.astral.express.pccms.appointment.entity.ServiceCatalog;
import com.astral.express.pccms.pet.entity.Pets;
import com.astral.express.pccms.room.entity.RoomType;
import com.astral.express.pccms.user.entity.Users;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AppointmentResponseAssemblerTest {

    private final AppointmentResponseAssembler assembler = new AppointmentResponseAssembler();

    @Test
    void should_FormatVetName_Correctly() {
        assertThat(assembler.formatVetName(null)).isEqualTo("Bác sĩ");
        assertThat(assembler.formatVetName("")).isEqualTo("Bác sĩ");
        assertThat(assembler.formatVetName("   ")).isEqualTo("Bác sĩ");
        assertThat(assembler.formatVetName("Hieu")).isEqualTo("BS. Hieu");
        assertThat(assembler.formatVetName("BS. Hieu")).isEqualTo("BS. Hieu");
    }

    @Test
    void should_FormatSlotLabel_Correctly() {
        TimeSlotResponse response = assembler.toTimeSlotResponse(
                LocalTime.of(8, 5), LocalTime.of(9, 30), true);
        assertThat(response.label()).isEqualTo("08:05 - 09:30");
    }

    @Test
    void should_MapAppointmentStatus_Correctly() {
        Appointment appointment = createMockAppointment(AppointmentStatus.PENDING);
        assertThat(assembler.toResponse(appointment, 1).statusLabel()).isEqualTo("Chờ tiếp nhận");

        appointment.setStatusCode(AppointmentStatus.CHECKED_IN);
        assertThat(assembler.toResponse(appointment, 1).statusLabel()).isEqualTo("Đang chờ khám");
    }

    @Test
    void should_MapGroomingStatus_Correctly() {
        GroomingTicket ticket = new GroomingTicket();
        ticket.setStatusCode(GroomingStatus.IN_SERVICE);
        ticket.setAppointment(createMockAppointment(AppointmentStatus.IN_PROGRESS));
        
        GroomingBoardCardResponse response = assembler.toGroomingBoardCard(ticket);
        assertThat(response.statusLabel()).isEqualTo("Đang dùng dịch vụ");
    }

    @Test
    void should_MapBoardingStatus_Correctly() {
        BoardingBooking booking = new BoardingBooking();
        booking.setStatusCode(BoardingStatus.RESERVED);
        
        Pets pet = new Pets();
        pet.setId(UUID.randomUUID());
        pet.setName("Kiki");
        booking.setPet(pet);
        
        RoomType rt = new RoomType();
        rt.setName("VIP");
        booking.setRequestedRoomType(rt);

        assertThat(assembler.toBoardingResponse(booking).statusLabel()).isEqualTo("Đã đặt phòng");
    }

    private Appointment createMockAppointment(AppointmentStatus status) {
        Appointment appointment = new Appointment();
        appointment.setStatusCode(status);
        ServiceOrder order = new ServiceOrder();
        Users owner = new Users();
        owner.setFullName("John Doe");
        order.setOwner(owner);
        Pets pet = new Pets();
        pet.setName("Dog");
        order.setPet(pet);
        ServiceCatalog service = new ServiceCatalog();
        service.setName("Vaccine");
        order.setService(service);
        appointment.setServiceOrder(order);
        return appointment;
    }

    @Test
    void should_MapAppointmentResponse_WithVet() {
        Appointment appointment = createMockAppointment(AppointmentStatus.CONFIRMED);
        Users vet = new Users();
        vet.setId(UUID.randomUUID());
        vet.setFullName("Jane Doe");
        appointment.setAssignedStaff(vet);

        AppointmentResponse response = assembler.toResponse(appointment, 5);
        assertThat(response.assignedVetId()).isEqualTo(vet.getId());
        assertThat(response.assignedVetName()).isEqualTo("BS. Jane Doe");
        assertThat(response.queueNumber()).isEqualTo(5);
        assertThat(response.statusLabel()).isEqualTo("Đã xác nhận");
    }

    @Test
    void should_MapAppointmentResponse_AllStatuses() {
        assertThat(assembler.toResponse(createMockAppointment(AppointmentStatus.PENDING), 1).statusLabel()).isEqualTo("Chờ tiếp nhận");
        assertThat(assembler.toResponse(createMockAppointment(AppointmentStatus.CONFIRMED), 1).statusLabel()).isEqualTo("Đã xác nhận");
        assertThat(assembler.toResponse(createMockAppointment(AppointmentStatus.CHECKED_IN), 1).statusLabel()).isEqualTo("Đang chờ khám");
        assertThat(assembler.toResponse(createMockAppointment(AppointmentStatus.IN_PROGRESS), 1).statusLabel()).isEqualTo("Đang khám");
        assertThat(assembler.toResponse(createMockAppointment(AppointmentStatus.COMPLETED), 1).statusLabel()).isEqualTo("Hoàn thành");
        assertThat(assembler.toResponse(createMockAppointment(AppointmentStatus.CANCELLED), 1).statusLabel()).isEqualTo("Đã hủy");
    }

    @Test
    void should_MapGroomingBoardCard_AllStatusesAndStation() {
        GroomingTicket ticket = new GroomingTicket();
        ticket.setAppointment(createMockAppointment(AppointmentStatus.PENDING));

        com.astral.express.pccms.grooming.entity.GroomingStation station = new com.astral.express.pccms.grooming.entity.GroomingStation();
        station.setName("Station 1");
        ticket.setStation(station);

        ticket.setStatusCode(GroomingStatus.PENDING);
        assertThat(assembler.toGroomingBoardCard(ticket).statusLabel()).isEqualTo("Chờ làm");
        assertThat(assembler.toGroomingBoardCard(ticket).stationName()).isEqualTo("Station 1");

        ticket.setStatusCode(GroomingStatus.CONFIRMED);
        assertThat(assembler.toGroomingBoardCard(ticket).statusLabel()).isEqualTo("Đã xác nhận");

        ticket.setStatusCode(GroomingStatus.IN_SERVICE);
        assertThat(assembler.toGroomingBoardCard(ticket).statusLabel()).isEqualTo("Đang dùng dịch vụ");

        ticket.setStatusCode(GroomingStatus.COMPLETED);
        assertThat(assembler.toGroomingBoardCard(ticket).statusLabel()).isEqualTo("Hoàn thành");

        ticket.setStatusCode(GroomingStatus.CANCELLED);
        assertThat(assembler.toGroomingBoardCard(ticket).statusLabel()).isEqualTo("Đã hủy");
        
        ticket.setStation(null);
        assertThat(assembler.toGroomingBoardCard(ticket).stationName()).isNull();
    }

    @Test
    void should_MapBoardingResponse_AllStatusesAndPrices() {
        BoardingBooking booking = new BoardingBooking();
        Pets pet = new Pets();
        pet.setId(UUID.randomUUID());
        pet.setName("Kiki");
        booking.setPet(pet);
        RoomType rt = new RoomType();
        rt.setName("VIP");
        booking.setRequestedRoomType(rt);

        booking.setEstimatedPriceVnd(1000L);
        booking.setStatusCode(BoardingStatus.RESERVED);
        assertThat(assembler.toBoardingResponse(booking).statusLabel()).isEqualTo("Đã đặt phòng");
        assertThat(assembler.toBoardingResponse(booking).estimatedPriceVnd()).isEqualTo(java.math.BigDecimal.valueOf(1000L));

        booking.setEstimatedPriceVnd(null);
        booking.setStatusCode(BoardingStatus.CHECKED_IN);
        assertThat(assembler.toBoardingResponse(booking).statusLabel()).isEqualTo("Đã nhận phòng");
        assertThat(assembler.toBoardingResponse(booking).estimatedPriceVnd()).isEqualTo(java.math.BigDecimal.ZERO);

        booking.setStatusCode(BoardingStatus.IN_STAY);
        assertThat(assembler.toBoardingResponse(booking).statusLabel()).isEqualTo("Đang lưu trú");

        booking.setStatusCode(BoardingStatus.CHECKED_OUT);
        assertThat(assembler.toBoardingResponse(booking).statusLabel()).isEqualTo("Đã trả phòng");

        booking.setStatusCode(BoardingStatus.CANCELLED);
        assertThat(assembler.toBoardingResponse(booking).statusLabel()).isEqualTo("Đã hủy");
    }

    @Test
    void should_MapRoomTypeOption() {
        RoomType rt = new RoomType();
        rt.setId(UUID.randomUUID());
        rt.setCode("R1");
        rt.setName("Room 1");

        rt.setBaseDailyPriceVnd(500L);
        assertThat(assembler.toRoomTypeOptionResponse(rt).baseDailyPriceVnd()).isEqualTo(java.math.BigDecimal.valueOf(500L));

        rt.setBaseDailyPriceVnd(null);
        assertThat(assembler.toRoomTypeOptionResponse(rt).baseDailyPriceVnd()).isEqualTo(java.math.BigDecimal.ZERO);
    }

    @Test
    void should_MapServiceCatalogOption() {
        ServiceCatalog s = new ServiceCatalog();
        s.setId(UUID.randomUUID());
        s.setServiceCode("S1");
        s.setName("Service");
        s.setCategoryCode(com.astral.express.pccms.appointment.entity.ServiceCategory.MEDICAL);
        s.setBasePriceVnd(100L);
        s.setDurationMinutes(30);

        var r = assembler.toServiceCatalogOptionResponse(s);
        assertThat(r.id()).isEqualTo(s.getId());
        assertThat(r.basePriceVnd()).isEqualTo(100L);
    }

    @Test
    void should_MapVetOption() {
        Users vet = new Users();
        vet.setId(UUID.randomUUID());
        vet.setFullName("Jane");
        var r = assembler.toVetOptionResponse(vet, true);
        assertThat(r.id()).isEqualTo(vet.getId());
        assertThat(r.fullName()).isEqualTo("BS. Jane");
        assertThat(r.available()).isTrue();
    }

}