package com.astral.express.pccms.appointment.service;

import com.astral.express.pccms.appointment.entity.Appointment;
import com.astral.express.pccms.appointment.entity.AppointmentStatus;
import com.astral.express.pccms.appointment.entity.ReceptionTicket;
import com.astral.express.pccms.appointment.repository.ReceptionTicketRepository;
import com.astral.express.pccms.user.entity.Users;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ReceptionTicketServiceTest {

    @Mock
    private ReceptionTicketRepository receptionTicketRepository;

    @InjectMocks
    private ReceptionTicketService service;

    @Test
    void receiveAppointment_shouldReturnNextQueueAndSaveTicket() {
        Users staff = new Users();
        staff.setId(UUID.randomUUID());
        Users vet = new Users();
        vet.setId(UUID.randomUUID());
        Appointment appointment = new Appointment();

        given(receptionTicketRepository.findMaxQueueNumberForVet(eq(vet.getId()), any(), any())).willReturn(5);

        int result = service.receiveAppointment(appointment, staff, vet);

        assertThat(result).isEqualTo(6);
    }

    @Test
    void getQueueNumberForAppointment_shouldReturnNumber() {
        UUID appId = UUID.randomUUID();
        ReceptionTicket ticket = new ReceptionTicket();
        ticket.setQueueNumber(10);

        given(receptionTicketRepository.findByAppointmentId(appId)).willReturn(Optional.of(ticket));

        Integer result = service.getQueueNumberForAppointment(appId);

        assertThat(result).isEqualTo(10);
    }

    @Test
    void getQueueNumberForAppointment_shouldReturnNull_whenNotFound() {
        given(receptionTicketRepository.findByAppointmentId(any())).willReturn(Optional.empty());

        Integer result = service.getQueueNumberForAppointment(UUID.randomUUID());

        assertThat(result).isNull();
    }

    @Test
    void getQueueForVet_shouldReturnTickets() {
        UUID vetId = UUID.randomUUID();
        OffsetDateTime start = OffsetDateTime.now();
        OffsetDateTime end = start.plusMinutes(30);

        ReceptionTicket ticket = new ReceptionTicket();
        given(receptionTicketRepository.findVetQueueTickets(vetId, start, end, AppointmentStatus.CHECKED_IN))
                .willReturn(List.of(ticket));

        List<ReceptionTicket> result = service.getQueueForVet(vetId, start, end);

        assertThat(result).containsExactly(ticket);
    }
}
