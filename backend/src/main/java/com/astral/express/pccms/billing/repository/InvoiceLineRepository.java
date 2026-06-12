package com.astral.express.pccms.billing.repository;

import com.astral.express.pccms.billing.entity.InvoiceLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InvoiceLineRepository extends JpaRepository<InvoiceLine, UUID> {
    List<InvoiceLine> findByInvoiceIdOrderByLineOrderAsc(UUID invoiceId);
}
