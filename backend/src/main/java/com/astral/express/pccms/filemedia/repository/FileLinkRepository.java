package com.astral.express.pccms.filemedia.repository;

import com.astral.express.pccms.filemedia.entity.FileLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface FileLinkRepository extends JpaRepository<FileLink, UUID> {
}
