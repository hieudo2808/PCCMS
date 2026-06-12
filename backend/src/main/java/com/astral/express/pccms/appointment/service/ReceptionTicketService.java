package com.astral.express.pccms.appointment.service;

import com.astral.express.pccms.appointment.entity.Appointment;
import com.astral.express.pccms.appointment.entity.ReceptionTicket;
import com.astral.express.pccms.appointment.repository.ReceptionTicketRepository;
import com.astral.express.pccms.user.entity.Users;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReceptionTicketService {
    private final ReceptionTicketRepository receptionTicketRepository;

    public int receiveAppointment(Appointment appointment, Users staff, Users vet) {
        LocalDate today = ClinicDateTime.today();
        OffsetDateTime dayStart = ClinicDateTime.startOfDay(today);
        OffsetDateTime dayEnd = ClinicDateTime.endOfDay(today);
        int nextQueue = receptionTicketRepository.findMaxQueueNumberForVet(vet.getId(), dayStart, dayEnd) + 1;

        ReceptionTicket ticket = new ReceptionTicket();
        ticket.setAppointment(appointment);
        ticket.setCheckedInBy(staff);
        ticket.setCheckedInAt(ClinicDateTime.now());
        ticket.setQueueNumber(nextQueue);
        ticket.setAssignedVet(vet);
        receptionTicketRepository.save(ticket);

        return nextQueue;
    }

    public Integer getQueueNumberForAppointment(UUID appointmentId) {
        return receptionTicketRepository.findByAppointmentId(appointmentId)
                .map(ReceptionTicket::getQueueNumber)
                .orElse(null);
    }

    public List<ReceptionTicket> getQueueForVet(UUID vetId, OffsetDateTime start, OffsetDateTime end) {
        return receptionTicketRepository.findVetQueueTickets(vetId, start, end, com.astral.express.pccms.appointment.entity.AppointmentStatus.CHECKED_IN);
    }
}
