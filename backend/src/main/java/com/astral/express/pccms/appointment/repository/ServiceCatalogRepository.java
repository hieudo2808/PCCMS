package com.astral.express.pccms.appointment.repository;

import com.astral.express.pccms.appointment.entity.ServiceCatalog;
import com.astral.express.pccms.appointment.entity.ServiceCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ServiceCatalogRepository extends JpaRepository<ServiceCatalog, UUID> {
    Optional<ServiceCatalog> findByServiceCodeAndIsActiveTrue(String serviceCode);

    Optional<ServiceCatalog> findFirstByCategoryCodeAndIsActiveTrue(ServiceCategory categoryCode);

    List<ServiceCatalog> findByCategoryCodeAndIsActiveTrueOrderByNameAsc(ServiceCategory categoryCode);

    boolean existsByServiceCode(String serviceCode);

    boolean existsByServiceCodeAndIdNot(String serviceCode, UUID id);

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, UUID id);

    Page<ServiceCatalog> findByCategoryCode(ServiceCategory categoryCode, Pageable pageable);
}
