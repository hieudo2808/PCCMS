package com.astral.express.pccms.catalog.repository;

import com.astral.express.pccms.catalog.entity.Room;
import com.astral.express.pccms.catalog.entity.RoomStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface RoomRepository extends JpaRepository<Room, UUID> {
    boolean existsByRoomCode(String roomCode);

    boolean existsByRoomCodeAndIdNot(String roomCode, UUID id);

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, UUID id);

    Page<Room> findByRoomTypeId(UUID roomTypeId, Pageable pageable);

    Page<Room> findByStatusCode(RoomStatus statusCode, Pageable pageable);

    Page<Room> findByRoomTypeIdAndStatusCode(UUID roomTypeId, RoomStatus statusCode, Pageable pageable);

    boolean existsByRoomTypeId(UUID roomTypeId);
}
