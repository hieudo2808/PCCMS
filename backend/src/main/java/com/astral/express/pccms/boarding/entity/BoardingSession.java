package com.astral.express.pccms.boarding.entity;

import com.astral.express.pccms.common.domain.AuditableEntity;
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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "boarding_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BoardingSession extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false, unique = true)
    private BoardingBooking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_allocation_id")
    private RoomAllocation roomAllocation;

    @Column(name = "actual_checkin_at")
    private OffsetDateTime actualCheckinAt;

    @Column(name = "actual_checkout_at")
    private OffsetDateTime actualCheckoutAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "checked_in_by")
    private Users checkedInBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "checked_out_by")
    private Users checkedOutBy;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status_code", nullable = false)
    @Builder.Default
    private BoardingStatus statusCode = BoardingStatus.RESERVED;
}
