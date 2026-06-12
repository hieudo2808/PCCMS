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
}
