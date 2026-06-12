package com.astral.express.pccms.filemedia.repository;

import com.astral.express.pccms.filemedia.entity.FileAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface FileAssetRepository extends JpaRepository<FileAsset, UUID> {
}
