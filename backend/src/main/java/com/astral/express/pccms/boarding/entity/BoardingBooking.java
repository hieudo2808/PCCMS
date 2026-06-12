package com.astral.express.pccms.boarding.entity;

import com.astral.express.pccms.appointment.entity.ServiceOrder;
import com.astral.express.pccms.common.domain.AuditableEntity;
import com.astral.express.pccms.pet.entity.Pets;
import com.astral.express.pccms.room.entity.RoomType;
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

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "boarding_bookings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BoardingBooking extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "booking_code", nullable = false, unique = true, length = 60)
    private String bookingCode;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_order_id", nullable = false, unique = true)
    private ServiceOrder serviceOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private Users owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_id", nullable = false)
    private Pets pet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_room_type_id", nullable = false)
    private RoomType requestedRoomType;

    @Column(name = "expected_checkin_at", nullable = false)
    private OffsetDateTime expectedCheckinAt;

    @Column(name = "expected_checkout_at", nullable = false)
    private OffsetDateTime expectedCheckoutAt;

    @Column(name = "special_care_request", columnDefinition = "TEXT")
    private String specialCareRequest;

    @Column(name = "estimated_price_vnd", nullable = false)
    @Builder.Default
    private Long estimatedPriceVnd = 0L;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status_code", nullable = false)
    @Builder.Default
    private BoardingStatus statusCode = BoardingStatus.RESERVED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private Users createdBy;
}
