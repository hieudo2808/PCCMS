package com.astral.express.pccms.billing.service;

import com.astral.express.pccms.billing.dto.response.InvoiceLineResponse;
import com.astral.express.pccms.billing.dto.response.InvoiceResponse;
import com.astral.express.pccms.billing.entity.Invoice;
import com.astral.express.pccms.billing.entity.InvoiceLine;
import com.astral.express.pccms.billing.repository.InvoiceLineRepository;
import com.astral.express.pccms.billing.repository.InvoiceRepository;
import com.astral.express.pccms.billing.service.InvoiceService;
import com.astral.express.pccms.common.dto.PageResponse;
import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.identity.security.SecurityContextService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InvoiceService {
    private final InvoiceRepository invoiceRepository;
    private final InvoiceLineRepository invoiceLineRepository;
    private final SecurityContextService SecurityContextService;
public PageResponse<InvoiceResponse> listMyInvoices(Pageable pageable) {
        UUID currentUserId = requireCurrentUserId();
        return PageResponse.of(invoiceRepository.findByOwnerIdOrderByIssuedAtDesc(currentUserId, pageable)
                .map(this::toResponse));
    }
public PageResponse<InvoiceResponse> listInvoices(Pageable pageable) {
        return PageResponse.of(invoiceRepository.findAllByOrderByIssuedAtDesc(pageable).map(this::toResponse));
    }
public InvoiceResponse getInvoice(UUID invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_BILLING_002_INVOICE_NOT_FOUND));
        assertCanRead(invoice);
        return toResponse(invoice);
    }

    private void assertCanRead(Invoice invoice) {
        if (SecurityContextService.hasAnyRole("ADMIN", "STAFF")) {
            return;
        }
        UUID currentUserId = requireCurrentUserId();
        if (!invoice.getOwner().getId().equals(currentUserId)) {
            throw new BusinessException(ErrorCode.ERR_403_FORBIDDEN);
        }
    }

    private UUID requireCurrentUserId() {
        UUID currentUserId = SecurityContextService.getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException(ErrorCode.ERR_401_UNAUTHORIZED);
        }
        return currentUserId;
    }

    private InvoiceResponse toResponse(Invoice invoice) {
        List<InvoiceLineResponse> lines = invoiceLineRepository.findByInvoiceIdOrderByLineOrderAsc(invoice.getId())
                .stream()
                .map(this::toLineResponse)
                .toList();
        return new InvoiceResponse(
                invoice.getId(),
                invoice.getInvoiceCode(),
                invoice.getOwner().getId(),
                invoice.getPet() == null ? null : invoice.getPet().getId(),
                invoice.getStatusCode(),
                invoice.getTotalAmountVnd(),
                invoice.getPaidAmountVnd(),
                invoice.getIssuedAt(),
                invoice.getNote(),
                lines
        );
    }

    private InvoiceLineResponse toLineResponse(InvoiceLine line) {
        return new InvoiceLineResponse(
                line.getId(),
                line.getInvoice().getId(),
                line.getServiceOrder() == null ? null : line.getServiceOrder().getId(),
                line.getMedicine() == null ? null : line.getMedicine().getId(),
                line.getDescription(),
                line.getQuantity() == null ? null : line.getQuantity().intValue(),
                line.getUnitPriceVnd(),
                line.getSubtotalVnd()
        );
    }
}


