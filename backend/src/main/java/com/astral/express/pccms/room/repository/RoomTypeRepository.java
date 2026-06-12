package com.astral.express.pccms.room.repository;

import com.astral.express.pccms.room.entity.RoomType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoomTypeRepository extends JpaRepository<RoomType, UUID> {
    Page<RoomType> findByIsActiveTrue(Pageable pageable);

    List<RoomType> findByIsActiveTrueOrderByNameAsc();

    List<RoomType> findAllByOrderByNameAsc();

    Optional<RoomType> findByIdAndIsActiveTrue(UUID id);

    boolean existsByCodeAndIsActiveTrue(String code);

    boolean existsByCodeAndIdNot(String code, UUID id);
}
