package com.astral.express.pccms.billing.service;

import com.astral.express.pccms.billing.dto.request.RecordPaymentRequest;
import com.astral.express.pccms.billing.dto.request.OwnerPaymentRequest;
import com.astral.express.pccms.billing.dto.response.PaymentResponse;
import com.astral.express.pccms.billing.entity.Invoice;
import com.astral.express.pccms.billing.entity.InvoiceStatus;
import com.astral.express.pccms.billing.entity.Payment;
import com.astral.express.pccms.billing.entity.PaymentStatus;
import com.astral.express.pccms.billing.repository.InvoiceRepository;
import com.astral.express.pccms.billing.repository.PaymentRepository;
import com.astral.express.pccms.billing.service.PaymentService;
import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.identity.security.SecurityContextService;
import com.astral.express.pccms.user.entity.Users;
import com.astral.express.pccms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final SecurityContextService SecurityContextService;
@Transactional
    public PaymentResponse recordPayment(RecordPaymentRequest request) {
        if (request.amountVnd() == null || request.amountVnd() <= 0) {
            throw new BusinessException(ErrorCode.ERR_BILLING_003_INVALID_PAYMENT_AMOUNT);
        }

        Invoice invoice = invoiceRepository.findByIdForUpdate(request.invoiceId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_BILLING_002_INVOICE_NOT_FOUND));
        long remainingAmount = Math.max(0L, invoice.getTotalAmountVnd() - invoice.getPaidAmountVnd());
        if (remainingAmount == 0L || request.amountVnd() > remainingAmount) {
            throw new BusinessException(ErrorCode.ERR_BILLING_003_INVALID_PAYMENT_AMOUNT);
        }

        Users receiver = userRepository.findById(requireCurrentUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_ACC_002_USER_NOT_FOUND));
        OffsetDateTime paidAt = request.paidAt() == null ? OffsetDateTime.now() : request.paidAt();

        Payment payment = Payment.builder()
                .paymentCode(generatePaymentCode())
                .invoice(invoice)
                .amountVnd(request.amountVnd())
                .methodCode(request.methodCode())
                .statusCode(PaymentStatus.SUCCEEDED)
                .paidAt(paidAt)
                .receivedBy(receiver)
                .note(request.note())
                .build();
        Payment savedPayment = paymentRepository.save(payment);

        invoice.setPaidAmountVnd(invoice.getPaidAmountVnd() + request.amountVnd());
        invoice.setStatusCode(invoice.getPaidAmountVnd() >= invoice.getTotalAmountVnd()
                ? InvoiceStatus.PAID
                : InvoiceStatus.PARTIALLY_PAID);
        invoiceRepository.save(invoice);

        return toResponse(savedPayment);
    }
@Transactional
    public PaymentResponse createOwnerPaymentRequest(UUID invoiceId, OwnerPaymentRequest request) {
        if (request.amountVnd() == null || request.amountVnd() <= 0) {
            throw new BusinessException(ErrorCode.ERR_BILLING_003_INVALID_PAYMENT_AMOUNT);
        }

        Invoice invoice = invoiceRepository.findByIdForUpdate(invoiceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_BILLING_002_INVOICE_NOT_FOUND));
        UUID currentUserId = requireCurrentUserId();
        if (!invoice.getOwner().getId().equals(currentUserId)) {
            throw new BusinessException(ErrorCode.ERR_403_FORBIDDEN);
        }

        long remainingAmount = Math.max(0L, invoice.getTotalAmountVnd() - invoice.getPaidAmountVnd());
        if (remainingAmount == 0L || request.amountVnd() > remainingAmount) {
            throw new BusinessException(ErrorCode.ERR_BILLING_003_INVALID_PAYMENT_AMOUNT);
        }

        String note = buildOwnerPaymentNote(request);
        Payment payment = Payment.builder()
                .paymentCode(generatePaymentCode())
                .invoice(invoice)
                .amountVnd(request.amountVnd())
                .methodCode(request.methodCode())
                .statusCode(PaymentStatus.SUCCEEDED)
                .paidAt(OffsetDateTime.now())
                .note(note)
                .build();
        Payment savedPayment = paymentRepository.save(payment);
        
        applySucceededPayment(invoice, request.amountVnd());
        
        return toResponse(savedPayment);
    }
@Transactional
    public PaymentResponse updatePaymentStatus(UUID paymentId, PaymentStatus statusCode, String note) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_BILLING_002_INVOICE_NOT_FOUND));
        PaymentStatus previousStatus = payment.getStatusCode();
        payment.setStatusCode(statusCode);
        if (note != null && !note.isBlank()) {
            payment.setNote(note);
        }

        if (statusCode == PaymentStatus.SUCCEEDED && previousStatus != PaymentStatus.SUCCEEDED) {
            Users receiver = userRepository.findById(requireCurrentUserId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.ERR_ACC_002_USER_NOT_FOUND));
            payment.setReceivedBy(receiver);
            payment.setPaidAt(OffsetDateTime.now());
            applySucceededPayment(payment.getInvoice(), payment.getAmountVnd());
        }

        return toResponse(paymentRepository.save(payment));
    }

    private UUID requireCurrentUserId() {
        UUID currentUserId = SecurityContextService.getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException(ErrorCode.ERR_401_UNAUTHORIZED);
        }
        return currentUserId;
    }

    private PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getPaymentCode(),
                payment.getInvoice().getId(),
                payment.getAmountVnd(),
                payment.getMethodCode(),
                payment.getStatusCode(),
                payment.getPaidAt(),
                payment.getReceivedBy() == null ? null : payment.getReceivedBy().getId(),
                payment.getNote()
        );
    }

    private void applySucceededPayment(Invoice invoice, Long amountVnd) {
        long remainingAmount = Math.max(0L, invoice.getTotalAmountVnd() - invoice.getPaidAmountVnd());
        if (remainingAmount == 0L || amountVnd > remainingAmount) {
            throw new BusinessException(ErrorCode.ERR_BILLING_003_INVALID_PAYMENT_AMOUNT);
        }
        invoice.setPaidAmountVnd(invoice.getPaidAmountVnd() + amountVnd);
        invoice.setStatusCode(invoice.getPaidAmountVnd() >= invoice.getTotalAmountVnd()
                ? InvoiceStatus.PAID
                : InvoiceStatus.PARTIALLY_PAID);
        invoiceRepository.save(invoice);
    }

    private String buildOwnerPaymentNote(OwnerPaymentRequest request) {
        StringBuilder builder = new StringBuilder();
        if (request.referenceCode() != null && !request.referenceCode().isBlank()) {
            builder.append("Reference: ").append(request.referenceCode().trim());
        }
        if (request.proofFileId() != null) {
            if (!builder.isEmpty()) {
                builder.append("\n");
            }
            builder.append("Proof file: ").append(request.proofFileId());
        }
        if (request.note() != null && !request.note().isBlank()) {
            if (!builder.isEmpty()) {
                builder.append("\n");
            }
            builder.append(request.note().trim());
        }
        return builder.isEmpty() ? null : builder.toString();
    }

    private String generatePaymentCode() {
        return "PAY-" + OffsetDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
    }
}


