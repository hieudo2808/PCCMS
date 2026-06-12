package com.astral.express.pccms.boarding.entity;

import com.astral.express.pccms.room.entity.Room;
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
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "room_allocations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private BoardingBooking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(name = "allocated_from", nullable = false)
    private OffsetDateTime allocatedFrom;

    @Column(name = "allocated_to", nullable = false)
    private OffsetDateTime allocatedTo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "allocated_by")
    private Users allocatedBy;

    @Column(name = "released_at")
    private OffsetDateTime releasedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_code", nullable = false, length = 30)
    @Builder.Default
    private RoomAllocationStatus statusCode = RoomAllocationStatus.ALLOCATED;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();
}
