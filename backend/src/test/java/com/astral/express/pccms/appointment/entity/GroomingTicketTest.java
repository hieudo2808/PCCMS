package com.astral.express.pccms.appointment.entity;

import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.grooming.entity.GroomingStation;
import com.astral.express.pccms.user.entity.Users;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GroomingTicketTest {

    private GroomingTicket ticket;
    private Appointment appointment;
    private ServiceOrder serviceOrder;
    private Users assignedStaff;
    private GroomingStation station;
    private UUID actorId;

    @BeforeEach
    void setUp() {
        actorId = UUID.randomUUID();
        assignedStaff = new Users();
        assignedStaff.setId(UUID.randomUUID());

        station = new GroomingStation();
        station.setId(UUID.randomUUID());

        serviceOrder = new ServiceOrder();
        serviceOrder.setId(UUID.randomUUID());
        serviceOrder.setBaseAmountVnd(100000L);
        serviceOrder.setExtraAmountVnd(50000L);

        appointment = new Appointment();
        appointment.setId(UUID.randomUUID());
        appointment.setServiceOrder(serviceOrder);

        ticket = new GroomingTicket();
        ticket.setId(UUID.randomUUID());
        ticket.setAppointment(appointment);
        ticket.setStatusCode(GroomingStatus.PENDING);
    }

    @Test
    void should_TransitionToConfirmed_when_PendingStatus() {
        // Act
        ticket.confirm(station, assignedStaff, "Note", actorId);

        // Assert
        assertThat(ticket.getStatusCode()).isEqualTo(GroomingStatus.CONFIRMED);
        assertThat(ticket.getStation()).isEqualTo(station);
        assertThat(ticket.getAssignedStaff()).isEqualTo(assignedStaff);
        assertThat(ticket.getInternalNote()).isEqualTo("Note");
        
        assertThat(appointment.getStatusCode()).isEqualTo(AppointmentStatus.CONFIRMED);
        assertThat(appointment.getAssignedStaff()).isEqualTo(assignedStaff);
        
        assertThat(serviceOrder.getStatusCode()).isEqualTo(ServiceOrderStatus.CONFIRMED);
        assertThat(serviceOrder.getUpdatedBy()).isEqualTo(actorId);
    }

    @Test
    void should_ThrowException_when_ConfirmingFromInvalidStatus() {
        ticket.setStatusCode(GroomingStatus.CONFIRMED);

        assertThatThrownBy(() -> ticket.confirm(station, assignedStaff, "Note", actorId))
                .isInstanceOf(BusinessException.class);
    }
    
    @Test
    void should_TransitionToInService_when_ConfirmedWithStation() {
        ticket.setStatusCode(GroomingStatus.CONFIRMED);
        ticket.setStation(station);
        OffsetDateTime now = OffsetDateTime.now();

        ticket.start(now, actorId);

        assertThat(ticket.getStatusCode()).isEqualTo(GroomingStatus.IN_SERVICE);
        assertThat(ticket.getStartedAt()).isEqualTo(now);
        assertThat(appointment.getStatusCode()).isEqualTo(AppointmentStatus.IN_PROGRESS);
        assertThat(serviceOrder.getStatusCode()).isEqualTo(ServiceOrderStatus.IN_PROGRESS);
    }

    @Test
    void should_ThrowException_when_StartingWithoutStation() {
        ticket.setStatusCode(GroomingStatus.CONFIRMED);

        assertThatThrownBy(() -> ticket.start(OffsetDateTime.now(), actorId))
                .isInstanceOf(BusinessException.class);
    }
}
