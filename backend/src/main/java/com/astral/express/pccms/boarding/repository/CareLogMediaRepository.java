package com.astral.express.pccms.boarding.repository;

import com.astral.express.pccms.boarding.entity.CareLogMedia;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CareLogMediaRepository extends JpaRepository<CareLogMedia, UUID> {
    @EntityGraph(attributePaths = "file")
    List<CareLogMedia> findByCareLogId(UUID careLogId);
}
