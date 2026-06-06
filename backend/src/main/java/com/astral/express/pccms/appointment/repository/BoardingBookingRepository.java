package com.astral.express.pccms.appointment.repository;

import com.astral.express.pccms.appointment.entity.BoardingBooking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BoardingBookingRepository extends JpaRepository<BoardingBooking, UUID> {

    @Query("""
            SELECT bb FROM BoardingBooking bb
            JOIN FETCH bb.pet p
            JOIN FETCH bb.requestedRoomType rt
            WHERE bb.owner.id = :ownerId
            ORDER BY bb.expectedCheckinAt DESC
            """)
    List<BoardingBooking> findByOwnerId(@Param("ownerId") UUID ownerId);

    @Query(value = """
            SELECT COALESCE(MAX(CAST(SUBSTRING(booking_code FROM 3) AS INTEGER)), 0)
            FROM boarding_bookings
            WHERE booking_code ~ '^BR[0-9]+$'
            """, nativeQuery = true)
    long maxBookingSequence();
}
