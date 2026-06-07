package com.astral.express.pccms.boarding.repository;

import com.astral.express.pccms.boarding.entity.ServiceCatalog;
import com.astral.express.pccms.boarding.entity.ServiceCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

@Repository
public interface ServiceCatalogRepository extends JpaRepository<ServiceCatalog, UUID> {
    Optional<ServiceCatalog> findFirstByCategoryCodeAndIsActiveTrueOrderByCreatedAtDesc(ServiceCategory categoryCode);

    List<ServiceCatalog> findByCategoryCodeAndIsActiveTrueOrderByNameAsc(ServiceCategory categoryCode);

    List<ServiceCatalog> findByCategoryCodeOrderByNameAsc(ServiceCategory categoryCode);

    Optional<ServiceCatalog> findByIdAndCategoryCode(UUID id, ServiceCategory categoryCode);

    Optional<ServiceCatalog> findByIdAndCategoryCodeAndIsActiveTrue(UUID id, ServiceCategory categoryCode);

    boolean existsByServiceCode(String serviceCode);

    boolean existsByServiceCodeAndIdNot(String serviceCode, UUID id);
}
