package com.astral.express.pccms.billing.repository;

import com.astral.express.pccms.billing.entity.Payment;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    List<Payment> findByInvoiceIdOrderByPaidAtDescCreatedAtDesc(UUID invoiceId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select payment from Payment payment where payment.id = :paymentId")
    Optional<Payment> findByIdForUpdate(@Param("paymentId") UUID paymentId);
}
