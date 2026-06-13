package com.astral.express.pccms.billing.service;

import com.astral.express.pccms.billing.dto.request.RecordPaymentRequest;
import com.astral.express.pccms.billing.entity.Invoice;
import com.astral.express.pccms.billing.entity.InvoiceStatus;
import com.astral.express.pccms.billing.entity.PaymentMethod;
import com.astral.express.pccms.billing.repository.InvoiceRepository;
import com.astral.express.pccms.billing.repository.PaymentRepository;
import com.astral.express.pccms.common.AbstractIntegrationTest;
import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.identity.security.SecurityContextService;
import com.astral.express.pccms.user.entity.Roles;
import com.astral.express.pccms.user.entity.UserStatus;
import com.astral.express.pccms.user.entity.Users;
import com.astral.express.pccms.user.repository.RoleRepository;
import com.astral.express.pccms.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

class PaymentIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @MockitoBean
    private SecurityContextService securityContextService;

    private Users testUser;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
        invoiceRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();

        Roles role = Roles.builder()
                .code("TEST_OWNER")
                .name("Owner")
                .isActive(true)
                .build();
        roleRepository.save(role);

        Users user = Users.builder()
                .email("test.owner@pccms.vn")
                .passwordHash("hash")
                .fullName("Test Owner")
                .role(role)
                .statusCode(UserStatus.ACTIVE)
                .build();
        testUser = userRepository.saveAndFlush(user);

        given(securityContextService.getCurrentUserId()).willReturn(testUser.getId());
    }

    private Invoice createPendingInvoice(long totalAmount, long paidAmount) {
        Invoice invoice = Invoice.builder()
                .invoiceCode("INV-" + UUID.randomUUID().toString().substring(0, 8))
                .owner(testUser)
                .totalAmountVnd(totalAmount)
                .paidAmountVnd(paidAmount)
                .statusCode(InvoiceStatus.UNPAID)
                .build();
        return invoiceRepository.saveAndFlush(invoice);
    }

    @Test
    void should_recordPayment_persistPayment_and_updateInvoiceStatus_INT_BILL_001() {
        // Arrange
        Invoice invoice = createPendingInvoice(100000L, 0L);
        RecordPaymentRequest request = new RecordPaymentRequest(invoice.getId(), 50000L, PaymentMethod.CASH, OffsetDateTime.now(), "Test note");

        // Act
        paymentService.recordPayment(request);

        // Assert
        Invoice updatedInvoice = invoiceRepository.findById(invoice.getId()).orElseThrow();
        assertThat(updatedInvoice.getPaidAmountVnd()).isEqualTo(50000L);
        assertThat(updatedInvoice.getStatusCode()).isEqualTo(InvoiceStatus.PARTIALLY_PAID);

        long paymentCount = paymentRepository.count();
        assertThat(paymentCount).isEqualTo(1L);
    }

    @Test
    void should_not_markInvoicePaid_when_invalidPaymentAmount_INT_BILL_002() {
        // Arrange
        Invoice invoice = createPendingInvoice(100000L, 0L);
        // Invalid amount (greater than total)
        RecordPaymentRequest request = new RecordPaymentRequest(invoice.getId(), 150000L, PaymentMethod.CASH, OffsetDateTime.now(), "Test note");

        // Act & Assert
        assertThatThrownBy(() -> paymentService.recordPayment(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_BILLING_003_INVALID_PAYMENT_AMOUNT);

        // Verify rollback / unchanged state
        Invoice updatedInvoice = invoiceRepository.findById(invoice.getId()).orElseThrow();
        assertThat(updatedInvoice.getPaidAmountVnd()).isEqualTo(0L);
        assertThat(updatedInvoice.getStatusCode()).isEqualTo(InvoiceStatus.UNPAID);

        long paymentCount = paymentRepository.count();
        assertThat(paymentCount).isZero();
    }

    @Test
    void should_rollback_when_invoiceNotFound_INT_BILL_004() {
        // Arrange
        UUID fakeInvoiceId = UUID.randomUUID();
        RecordPaymentRequest request = new RecordPaymentRequest(fakeInvoiceId, 50000L, PaymentMethod.CASH, OffsetDateTime.now(), "Test note");

        // Act & Assert
        assertThatThrownBy(() -> paymentService.recordPayment(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_BILLING_002_INVOICE_NOT_FOUND);

        // Verify clean state
        long paymentCount = paymentRepository.count();
        assertThat(paymentCount).isZero();
    }

    @Test
    void should_reject_duplicatePayment_on_alreadyPaidInvoice_INT_BILL_003() {
        // Arrange
        Invoice invoice = createPendingInvoice(100000L, 100000L);
        invoice.setStatusCode(InvoiceStatus.PAID);
        invoiceRepository.saveAndFlush(invoice);

        RecordPaymentRequest request = new RecordPaymentRequest(invoice.getId(), 50000L, PaymentMethod.CASH, OffsetDateTime.now(), "Test note");

        // Act & Assert
        assertThatThrownBy(() -> paymentService.recordPayment(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_BILLING_003_INVALID_PAYMENT_AMOUNT);

        // Verify unchanged
        Invoice updatedInvoice = invoiceRepository.findById(invoice.getId()).orElseThrow();
        assertThat(updatedInvoice.getStatusCode()).isEqualTo(InvoiceStatus.PAID);
        assertThat(updatedInvoice.getPaidAmountVnd()).isEqualTo(100000L);

        long paymentCount = paymentRepository.count();
        assertThat(paymentCount).isZero();
    }
}
