package com.astral.express.pccms.billing.repository;

import com.astral.express.pccms.billing.entity.Invoice;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {
    Page<Invoice> findByOwnerIdOrderByIssuedAtDesc(UUID ownerId, Pageable pageable);

    Page<Invoice> findAllByOrderByIssuedAtDesc(Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Invoice i WHERE i.id = :invoiceId")
    Optional<Invoice> findByIdForUpdate(@Param("invoiceId") UUID invoiceId);

    @Query("""
            SELECT CASE WHEN COUNT(il) > 0 THEN true ELSE false END
            FROM InvoiceLine il
            WHERE il.serviceOrder.id = :serviceOrderId
            """)
    boolean existsByServiceOrderId(@Param("serviceOrderId") UUID serviceOrderId);

    @Query("""
            SELECT il.invoice
            FROM InvoiceLine il
            WHERE il.serviceOrder.id = :serviceOrderId
            """)
    Optional<Invoice> findByServiceOrderId(@Param("serviceOrderId") UUID serviceOrderId);
}
