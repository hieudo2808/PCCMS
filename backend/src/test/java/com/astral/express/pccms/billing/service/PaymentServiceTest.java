package com.astral.express.pccms.billing.service;

import com.astral.express.pccms.billing.dto.request.OwnerPaymentRequest;
import com.astral.express.pccms.billing.dto.request.RecordPaymentRequest;
import com.astral.express.pccms.billing.dto.response.PaymentResponse;
import com.astral.express.pccms.billing.entity.Invoice;
import com.astral.express.pccms.billing.entity.InvoiceStatus;
import com.astral.express.pccms.billing.entity.Payment;
import com.astral.express.pccms.billing.entity.PaymentMethod;
import com.astral.express.pccms.billing.entity.PaymentStatus;
import com.astral.express.pccms.billing.repository.InvoiceRepository;
import com.astral.express.pccms.billing.repository.PaymentRepository;
import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.identity.security.SecurityContextService;
import com.astral.express.pccms.user.entity.Users;
import com.astral.express.pccms.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecurityContextService securityContextService;

    @InjectMocks
    private PaymentService paymentService;

    @ParameterizedTest(name = "[{0}] {1}: {2} (amount={3})")
    @CsvFileSource(resources = "/testcases/billing-payment-service.csv", numLinesToSkip = 1)
    void should_ProcessPaymentLogic_AccordingToRules(
            String testCaseId, String method, String scenario,
            Long amount, Long totalAmount, Long paidAmount,
            boolean isOwner, boolean currentUserExists, boolean invoiceExists, boolean paymentExists,
            String targetStatusStr, String expectedResult, String expectedErrorCode, String expectedInvoiceStatus) {

        // GIVEN
        UUID invoiceId = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();
        UUID ownerId = isOwner ? currentUserId : UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();

        Users owner = new Users();
        owner.setId(ownerId);

        Users receiver = new Users();
        receiver.setId(currentUserId);

        Invoice mockInvoice = new Invoice();
        mockInvoice.setId(invoiceId);
        mockInvoice.setOwner(owner);
        mockInvoice.setTotalAmountVnd(totalAmount);
        mockInvoice.setPaidAmountVnd(paidAmount);
        mockInvoice.setStatusCode(InvoiceStatus.UNPAID);

        Payment mockPayment = new Payment();
        mockPayment.setId(paymentId);
        mockPayment.setInvoice(mockInvoice);
        mockPayment.setStatusCode(PaymentStatus.PENDING);
        mockPayment.setAmountVnd(amount == null || amount <= 0 ? 1000L : amount); // Valid initial payment amount

        PaymentStatus targetStatus = targetStatusStr != null && !targetStatusStr.isBlank() ? PaymentStatus.valueOf(targetStatusStr) : null;

        if ("recordPayment".equals(method)) {
            if (amount != null && amount > 0) {
                given(invoiceRepository.findByIdForUpdate(invoiceId))
                        .willReturn(invoiceExists ? Optional.of(mockInvoice) : Optional.empty());
                
                if (invoiceExists) {
                    long remainingAmount = Math.max(0L, mockInvoice.getTotalAmountVnd() - mockInvoice.getPaidAmountVnd());
                    if (remainingAmount > 0 && amount <= remainingAmount) {
                        given(securityContextService.getCurrentUserId()).willReturn(currentUserId);
                        given(userRepository.findById(currentUserId))
                                .willReturn(currentUserExists ? Optional.of(receiver) : Optional.empty());
                    }
                }
            }
        } else if ("createOwnerPaymentRequest".equals(method)) {
            if (amount != null && amount > 0) {
                given(invoiceRepository.findByIdForUpdate(invoiceId))
                        .willReturn(invoiceExists ? Optional.of(mockInvoice) : Optional.empty());
                
                if (invoiceExists) {
                    given(securityContextService.getCurrentUserId()).willReturn(currentUserId);
                }
            }
        } else if ("updatePaymentStatus".equals(method)) {
            given(paymentRepository.findById(paymentId))
                    .willReturn(paymentExists ? Optional.of(mockPayment) : Optional.empty());
            
            if (paymentExists && targetStatus == PaymentStatus.SUCCEEDED && mockPayment.getStatusCode() != PaymentStatus.SUCCEEDED) {
                given(securityContextService.getCurrentUserId()).willReturn(currentUserId);
                given(userRepository.findById(currentUserId))
                        .willReturn(currentUserExists ? Optional.of(receiver) : Optional.empty());
            }
        }

        if ("SUCCESS".equals(expectedResult)) {
            Payment savedPayment = new Payment();
            savedPayment.setId(UUID.randomUUID());
            savedPayment.setInvoice(mockInvoice);
            savedPayment.setAmountVnd(amount);
            savedPayment.setStatusCode(targetStatus);
            savedPayment.setPaymentCode("PAY-TEST");

            given(paymentRepository.save(any(Payment.class))).willReturn(savedPayment);

            // WHEN
            PaymentResponse response = null;
            if ("recordPayment".equals(method)) {
                RecordPaymentRequest request = new RecordPaymentRequest(invoiceId, amount, PaymentMethod.CASH, OffsetDateTime.now(), "Note");
                response = paymentService.recordPayment(request);
            } else if ("createOwnerPaymentRequest".equals(method)) {
                OwnerPaymentRequest request = new OwnerPaymentRequest(amount, PaymentMethod.BANK_TRANSFER, "REF", "Note", null);
                response = paymentService.createOwnerPaymentRequest(invoiceId, request);
            } else if ("updatePaymentStatus".equals(method)) {
                response = paymentService.updatePaymentStatus(paymentId, targetStatus, "Note");
            }

            // THEN
            assertThat(response).isNotNull();
            assertThat(response.statusCode()).isEqualTo(targetStatus);
            
            if ("updatePaymentStatus".equals(method)) {
                ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
                verify(paymentRepository).save(paymentCaptor.capture());
                Payment capturedPayment = paymentCaptor.getValue();
                assertThat(capturedPayment.getStatusCode()).isEqualTo(targetStatus);
            } else {
                verify(paymentRepository).save(any(Payment.class));
            }
            
            if (expectedInvoiceStatus != null && !expectedInvoiceStatus.isBlank()) {
                verify(invoiceRepository).save(mockInvoice);
                assertThat(mockInvoice.getStatusCode()).isEqualTo(InvoiceStatus.valueOf(expectedInvoiceStatus));
            }
            
        } else if ("ERROR".equals(expectedResult)) {
            // WHEN & THEN
            assertThatThrownBy(() -> {
                if ("recordPayment".equals(method)) {
                    RecordPaymentRequest request = new RecordPaymentRequest(invoiceId, amount, PaymentMethod.CASH, OffsetDateTime.now(), "Note");
                    paymentService.recordPayment(request);
                } else if ("createOwnerPaymentRequest".equals(method)) {
                    OwnerPaymentRequest request = new OwnerPaymentRequest(amount, PaymentMethod.BANK_TRANSFER, "REF", "Note", null);
                    paymentService.createOwnerPaymentRequest(invoiceId, request);
                } else if ("updatePaymentStatus".equals(method)) {
                    paymentService.updatePaymentStatus(paymentId, targetStatus, "Note");
                }
            })
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.valueOf(expectedErrorCode));

            verify(paymentRepository, never()).save(any(Payment.class));
            if ("updatePaymentStatus".equals(method) || "createOwnerPaymentRequest".equals(method) || "recordPayment".equals(method)) {
                verify(invoiceRepository, never()).save(any(Invoice.class));
            }
        }
    }

    @Test
    void should_createOwnerPaymentRequest_WithNoteAndProofFile() {
        UUID invoiceId = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();
        
        Users owner = new Users();
        owner.setId(currentUserId);
        
        Invoice invoice = new Invoice();
        invoice.setId(invoiceId);
        invoice.setOwner(owner);
        invoice.setTotalAmountVnd(1000L);
        invoice.setPaidAmountVnd(0L);
        
        given(invoiceRepository.findByIdForUpdate(invoiceId)).willReturn(Optional.of(invoice));
        given(securityContextService.getCurrentUserId()).willReturn(currentUserId);
        
        Payment savedPayment = new Payment();
        savedPayment.setId(UUID.randomUUID());
        savedPayment.setInvoice(invoice);
        given(paymentRepository.save(any(Payment.class))).willReturn(savedPayment);
        
        OwnerPaymentRequest request = new OwnerPaymentRequest(1000L, PaymentMethod.BANK_TRANSFER, "REF123", "Some Note", UUID.randomUUID());
        paymentService.createOwnerPaymentRequest(invoiceId, request);
        
        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());
        
        String note = paymentCaptor.getValue().getNote();
        assertThat(note).contains("Reference: REF123");
        assertThat(note).contains("Proof file: ");
        assertThat(note).contains("Some Note");
    }

    @Test
    void should_recordPayment_WithNullPaidAt() {
        UUID invoiceId = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();
        
        Invoice invoice = new Invoice();
        invoice.setId(invoiceId);
        invoice.setTotalAmountVnd(1000L);
        invoice.setPaidAmountVnd(0L);
        
        Users receiver = new Users();
        receiver.setId(currentUserId);
        
        given(invoiceRepository.findByIdForUpdate(invoiceId)).willReturn(Optional.of(invoice));
        given(securityContextService.getCurrentUserId()).willReturn(currentUserId);
        given(userRepository.findById(currentUserId)).willReturn(Optional.of(receiver));
        
        Payment savedPayment = new Payment();
        savedPayment.setId(UUID.randomUUID());
        savedPayment.setInvoice(invoice);
        given(paymentRepository.save(any(Payment.class))).willReturn(savedPayment);
        
        RecordPaymentRequest request = new RecordPaymentRequest(invoiceId, 1000L, PaymentMethod.CASH, null, "Note");
        paymentService.recordPayment(request);
        
        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());
        assertThat(paymentCaptor.getValue().getPaidAt()).isNotNull();
    }

    @Test
    void should_recordPayment_WithNonNullPaidAt() {
        UUID invoiceId = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();
        
        Invoice invoice = new Invoice();
        invoice.setId(invoiceId);
        invoice.setTotalAmountVnd(1000L);
        invoice.setPaidAmountVnd(0L);
        
        Users receiver = new Users();
        receiver.setId(currentUserId);
        
        given(invoiceRepository.findByIdForUpdate(invoiceId)).willReturn(Optional.of(invoice));
        given(securityContextService.getCurrentUserId()).willReturn(currentUserId);
        given(userRepository.findById(currentUserId)).willReturn(Optional.of(receiver));
        
        Payment savedPayment = new Payment();
        savedPayment.setId(UUID.randomUUID());
        savedPayment.setInvoice(invoice);
        given(paymentRepository.save(any(Payment.class))).willReturn(savedPayment);
        
        OffsetDateTime paidAt = OffsetDateTime.now().minusDays(1);
        RecordPaymentRequest request = new RecordPaymentRequest(invoiceId, 1000L, PaymentMethod.CASH, paidAt, "Note");
        paymentService.recordPayment(request);
        
        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());
        assertThat(paymentCaptor.getValue().getPaidAt()).isEqualTo(paidAt);
    }

    @Test
    void should_ThrowException_When_AmountIsNull() {
        RecordPaymentRequest request = new RecordPaymentRequest(UUID.randomUUID(), null, PaymentMethod.CASH, null, "Note");
        assertThatThrownBy(() -> paymentService.recordPayment(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_BILLING_003_INVALID_PAYMENT_AMOUNT);

        OwnerPaymentRequest ownerReq = new OwnerPaymentRequest(null, PaymentMethod.BANK_TRANSFER, "REF", "Note", null);
        assertThatThrownBy(() -> paymentService.createOwnerPaymentRequest(UUID.randomUUID(), ownerReq))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_BILLING_003_INVALID_PAYMENT_AMOUNT);
    }

    @Test
    void should_updatePaymentStatus_WithBlankNote() {
        UUID paymentId = UUID.randomUUID();
        Payment payment = new Payment();
        payment.setId(paymentId);
        payment.setStatusCode(PaymentStatus.PENDING);
        
        Invoice invoice = new Invoice();
        invoice.setId(UUID.randomUUID());
        payment.setInvoice(invoice);
        
        given(paymentRepository.findById(paymentId)).willReturn(Optional.of(payment));
        given(paymentRepository.save(any())).willAnswer(i -> i.getArgument(0));

        PaymentResponse response = paymentService.updatePaymentStatus(paymentId, PaymentStatus.FAILED, "   ");
        assertThat(response.statusCode()).isEqualTo(PaymentStatus.FAILED);
        // Note is not updated because it's blank
    }

    @Test
    void should_ThrowException_When_UpdatePaymentStatus_ExceedsRemainingAmount() {
        UUID paymentId = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();
        
        Invoice invoice = new Invoice();
        invoice.setTotalAmountVnd(1000L);
        invoice.setPaidAmountVnd(500L); // Remaining 500
        
        Payment payment = new Payment();
        payment.setId(paymentId);
        payment.setStatusCode(PaymentStatus.PENDING);
        payment.setAmountVnd(1000L); // Greater than remaining 500
        payment.setInvoice(invoice);
        
        Users receiver = new Users();
        receiver.setId(currentUserId);
        
        given(paymentRepository.findById(paymentId)).willReturn(Optional.of(payment));
        given(securityContextService.getCurrentUserId()).willReturn(currentUserId);
        given(userRepository.findById(currentUserId)).willReturn(Optional.of(receiver));

        assertThatThrownBy(() -> paymentService.updatePaymentStatus(paymentId, PaymentStatus.SUCCEEDED, null))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_BILLING_003_INVALID_PAYMENT_AMOUNT);
    }

    @Test
    void should_updatePaymentStatus_AndApplyPartialPayment() {
        UUID paymentId = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();
        
        Invoice invoice = new Invoice();
        invoice.setTotalAmountVnd(1000L);
        invoice.setPaidAmountVnd(0L); 
        
        Payment payment = new Payment();
        payment.setId(paymentId);
        payment.setStatusCode(PaymentStatus.PENDING);
        payment.setAmountVnd(400L); // Partial
        payment.setInvoice(invoice);
        
        Users receiver = new Users();
        receiver.setId(currentUserId);
        
        given(paymentRepository.findById(paymentId)).willReturn(Optional.of(payment));
        given(securityContextService.getCurrentUserId()).willReturn(currentUserId);
        given(userRepository.findById(currentUserId)).willReturn(Optional.of(receiver));
        given(paymentRepository.save(any())).willAnswer(i -> i.getArgument(0));

        paymentService.updatePaymentStatus(paymentId, PaymentStatus.SUCCEEDED, null);
        
        assertThat(invoice.getPaidAmountVnd()).isEqualTo(400L);
        assertThat(invoice.getStatusCode()).isEqualTo(InvoiceStatus.PARTIALLY_PAID);
    }

    @Test
    void should_createOwnerPaymentRequest_WithEmptyNote() {
        UUID invoiceId = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();
        
        Users owner = new Users();
        owner.setId(currentUserId);
        
        Invoice invoice = new Invoice();
        invoice.setId(invoiceId);
        invoice.setOwner(owner);
        invoice.setTotalAmountVnd(1000L);
        invoice.setPaidAmountVnd(0L);
        
        given(invoiceRepository.findByIdForUpdate(invoiceId)).willReturn(Optional.of(invoice));
        given(securityContextService.getCurrentUserId()).willReturn(currentUserId);
        
        Payment savedPayment = new Payment();
        savedPayment.setId(UUID.randomUUID());
        savedPayment.setInvoice(invoice);
        given(paymentRepository.save(any(Payment.class))).willReturn(savedPayment);
        
        OwnerPaymentRequest request = new OwnerPaymentRequest(1000L, PaymentMethod.BANK_TRANSFER, "   ", "   ", null);
        paymentService.createOwnerPaymentRequest(invoiceId, request);
        
        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());
        
        assertThat(paymentCaptor.getValue().getNote()).isNull();
    }

}