package com.astral.express.pccms.appointment.entity;

import com.astral.express.pccms.common.domain.AuditableEntity;
import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.grooming.entity.GroomingStation;
import com.astral.express.pccms.user.entity.Users;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "grooming_tickets")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Setter
public class GroomingTicket extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id", nullable = false, unique = true)
    private Appointment appointment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_staff_id")
    private Users assignedStaff;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "station_id")
    private GroomingStation station;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status_code", nullable = false)
    private GroomingStatus statusCode = GroomingStatus.PENDING;

    @Column(name = "started_at")
    private OffsetDateTime startedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "owner_note")
    private String ownerNote;

    @Column(name = "internal_note")
    private String internalNote;

    public void confirm(GroomingStation station, Users assignedStaff, String internalNote, UUID actorId) {
        if (this.statusCode != GroomingStatus.PENDING) {
            throw new BusinessException(ErrorCode.ERR_GROOMING_004_INVALID_STATUS_TRANSITION);
        }
        this.station = station;
        this.assignedStaff = assignedStaff;
        this.internalNote = internalNote;
        this.statusCode = GroomingStatus.CONFIRMED;

        this.appointment.setAssignedStaff(assignedStaff);
        this.appointment.setInternalNote(internalNote);
        this.appointment.setStatusCode(AppointmentStatus.CONFIRMED);

        this.appointment.getServiceOrder().setStatusCode(ServiceOrderStatus.CONFIRMED);
        this.appointment.getServiceOrder().setUpdatedBy(actorId);
    }

    public void start(OffsetDateTime startedAt, UUID actorId) {
        if (this.statusCode != GroomingStatus.CONFIRMED) {
            throw new BusinessException(ErrorCode.ERR_GROOMING_004_INVALID_STATUS_TRANSITION);
        }
        if (this.station == null) {
            throw new BusinessException(ErrorCode.ERR_GROOMING_006_STATION_UNAVAILABLE);
        }
        this.statusCode = GroomingStatus.IN_SERVICE;
        this.startedAt = startedAt;

        this.appointment.setStatusCode(AppointmentStatus.IN_PROGRESS);

        this.appointment.getServiceOrder().setStatusCode(ServiceOrderStatus.IN_PROGRESS);
        this.appointment.getServiceOrder().setActualStartAt(startedAt);
        this.appointment.getServiceOrder().setUpdatedBy(actorId);
    }

    public void complete(OffsetDateTime completedAt, String internalNote, UUID actorId) {
        if (this.statusCode == GroomingStatus.COMPLETED) {
            return;
        }
        if (this.statusCode != GroomingStatus.IN_SERVICE) {
            throw new BusinessException(ErrorCode.ERR_GROOMING_004_INVALID_STATUS_TRANSITION);
        }
        this.statusCode = GroomingStatus.COMPLETED;
        this.completedAt = completedAt;
        if (internalNote != null) {
            this.internalNote = internalNote;
        }
        
        Long extraAmount = this.appointment.getServiceOrder().getExtraAmountVnd();
        Long finalAmount = this.appointment.getServiceOrder().getBaseAmountVnd() + (extraAmount != null ? extraAmount : 0L);

        this.appointment.setStatusCode(AppointmentStatus.COMPLETED);
        this.appointment.setInternalNote(this.internalNote);

        this.appointment.getServiceOrder().setStatusCode(ServiceOrderStatus.COMPLETED);
        this.appointment.getServiceOrder().setCompletedAt(completedAt);
        this.appointment.getServiceOrder().setFinalAmountVnd(finalAmount);
        this.appointment.getServiceOrder().setUpdatedBy(actorId);
    }

    public void cancel(String reason, OffsetDateTime cancelledAt, UUID actorId, boolean isStaff) {
        if (this.statusCode == GroomingStatus.COMPLETED || this.statusCode == GroomingStatus.CANCELLED) {
            throw new BusinessException(ErrorCode.ERR_GROOMING_004_INVALID_STATUS_TRANSITION);
        }
        if (!isStaff && this.statusCode != GroomingStatus.PENDING) {
            throw new BusinessException(ErrorCode.ERR_GROOMING_004_INVALID_STATUS_TRANSITION);
        }
        this.statusCode = GroomingStatus.CANCELLED;
        this.internalNote = reason;

        this.appointment.setStatusCode(AppointmentStatus.CANCELLED);
        this.appointment.setInternalNote(reason);

        this.appointment.getServiceOrder().setStatusCode(ServiceOrderStatus.CANCELLED);
        this.appointment.getServiceOrder().setCancelledAt(cancelledAt);
        this.appointment.getServiceOrder().setCancellationReason(reason);
        this.appointment.getServiceOrder().setUpdatedBy(actorId);
    }
}

