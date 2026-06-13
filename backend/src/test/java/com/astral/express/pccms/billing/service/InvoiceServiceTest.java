package com.astral.express.pccms.billing.service;

import com.astral.express.pccms.billing.dto.response.InvoiceResponse;
import com.astral.express.pccms.billing.entity.Invoice;
import com.astral.express.pccms.billing.entity.InvoiceLine;
import com.astral.express.pccms.billing.entity.InvoiceStatus;
import com.astral.express.pccms.billing.repository.InvoiceLineRepository;
import com.astral.express.pccms.billing.repository.InvoiceRepository;
import com.astral.express.pccms.common.dto.PageResponse;
import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.identity.security.SecurityContextService;
import com.astral.express.pccms.user.entity.Users;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InvoiceServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;
    
    @Mock
    private InvoiceLineRepository invoiceLineRepository;

    @Mock
    private SecurityContextService securityContextService;

    @InjectMocks
    private InvoiceService invoiceService;

    @Test
    void should_ListMyInvoices_Success() {
        // GIVEN
        UUID currentUserId = UUID.randomUUID();
        given(securityContextService.getCurrentUserId()).willReturn(currentUserId);

        Invoice invoice = new Invoice();
        invoice.setId(UUID.randomUUID());
        invoice.setInvoiceCode("INV-001");
        Users owner = new Users();
        owner.setId(currentUserId);
        invoice.setOwner(owner);
        invoice.setTotalAmountVnd(100L);
        invoice.setPaidAmountVnd(0L);
        invoice.setStatusCode(InvoiceStatus.DRAFT);
        
        given(invoiceRepository.findByOwnerIdOrderByIssuedAtDesc(eq(currentUserId), any(PageRequest.class)))
                .willReturn(new PageImpl<>(List.of(invoice)));
        given(invoiceLineRepository.findByInvoiceIdOrderByLineOrderAsc(invoice.getId()))
                .willReturn(List.of());

        // WHEN
        PageResponse<InvoiceResponse> response = invoiceService.listMyInvoices(PageRequest.of(0, 10));

        // THEN
        assertThat(response.data().content()).hasSize(1);
        assertThat(response.data().content().get(0).invoiceCode()).isEqualTo("INV-001");
    }

    @Test
    void should_ListInvoices_Success() {
        // GIVEN
        Invoice invoice = new Invoice();
        invoice.setId(UUID.randomUUID());
        invoice.setInvoiceCode("INV-002");
        Users owner = new Users();
        owner.setId(UUID.randomUUID());
        invoice.setOwner(owner);
        invoice.setTotalAmountVnd(200L);
        invoice.setPaidAmountVnd(0L);
        invoice.setStatusCode(InvoiceStatus.DRAFT);
        
        given(invoiceRepository.findAllByOrderByIssuedAtDesc(any(PageRequest.class)))
                .willReturn(new PageImpl<>(List.of(invoice)));
        given(invoiceLineRepository.findByInvoiceIdOrderByLineOrderAsc(invoice.getId()))
                .willReturn(List.of());

        // WHEN
        PageResponse<InvoiceResponse> response = invoiceService.listInvoices(PageRequest.of(0, 10));

        // THEN
        assertThat(response.data().content()).hasSize(1);
        assertThat(response.data().content().get(0).invoiceCode()).isEqualTo("INV-002");
    }

    @Test
    void should_GetInvoice_when_UserIsAdmin_Success() {
        // GIVEN
        UUID invoiceId = UUID.randomUUID();
        Invoice invoice = new Invoice();
        invoice.setId(invoiceId);
        Users owner = new Users();
        owner.setId(UUID.randomUUID());
        invoice.setOwner(owner);
        
        given(invoiceRepository.findById(invoiceId)).willReturn(Optional.of(invoice));
        given(securityContextService.hasAnyRole("ADMIN", "STAFF")).willReturn(true);
        given(invoiceLineRepository.findByInvoiceIdOrderByLineOrderAsc(invoiceId)).willReturn(List.of());

        // WHEN
        InvoiceResponse response = invoiceService.getInvoice(invoiceId);

        // THEN
        assertThat(response.id()).isEqualTo(invoiceId);
    }

    @Test
    void should_GetInvoice_when_UserIsOwner_Success() {
        // GIVEN
        UUID invoiceId = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();
        Invoice invoice = new Invoice();
        invoice.setId(invoiceId);
        Users owner = new Users();
        owner.setId(currentUserId);
        invoice.setOwner(owner);
        
        given(invoiceRepository.findById(invoiceId)).willReturn(Optional.of(invoice));
        given(securityContextService.hasAnyRole("ADMIN", "STAFF")).willReturn(false);
        given(securityContextService.getCurrentUserId()).willReturn(currentUserId);
        given(invoiceLineRepository.findByInvoiceIdOrderByLineOrderAsc(invoiceId)).willReturn(List.of());

        // WHEN
        InvoiceResponse response = invoiceService.getInvoice(invoiceId);

        // THEN
        assertThat(response.id()).isEqualTo(invoiceId);
    }

    @Test
    void should_ThrowException_when_GetInvoiceAndUserIsNotOwner() {
        // GIVEN
        UUID invoiceId = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        
        Invoice invoice = new Invoice();
        invoice.setId(invoiceId);
        Users owner = new Users();
        owner.setId(otherUserId);
        invoice.setOwner(owner);
        
        given(invoiceRepository.findById(invoiceId)).willReturn(Optional.of(invoice));
        given(securityContextService.hasAnyRole("ADMIN", "STAFF")).willReturn(false);
        given(securityContextService.getCurrentUserId()).willReturn(currentUserId);

        // WHEN & THEN
        assertThatThrownBy(() -> invoiceService.getInvoice(invoiceId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_403_FORBIDDEN);
    }

    @Test
    void should_ThrowException_when_GetInvoice_NotFound() {
        UUID invoiceId = UUID.randomUUID();
        given(invoiceRepository.findById(invoiceId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> invoiceService.getInvoice(invoiceId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_BILLING_002_INVOICE_NOT_FOUND);
    }

    @Test
    void should_ThrowException_when_RequireCurrentUserId_IsNull() {
        given(securityContextService.getCurrentUserId()).willReturn(null);

        assertThatThrownBy(() -> invoiceService.listMyInvoices(PageRequest.of(0, 10)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_401_UNAUTHORIZED);
    }

    @Test
    void should_GetInvoice_WithNullFields_Success() {
        // GIVEN
        UUID invoiceId = UUID.randomUUID();
        Invoice invoice = new Invoice();
        invoice.setId(invoiceId);
        Users owner = new Users();
        owner.setId(UUID.randomUUID());
        invoice.setOwner(owner);
        invoice.setPet(null);
        
        InvoiceLine line = new InvoiceLine();
        line.setId(UUID.randomUUID());
        line.setInvoice(invoice);
        line.setServiceOrder(null);
        line.setMedicine(null);
        line.setQuantity(null);
        
        given(invoiceRepository.findById(invoiceId)).willReturn(Optional.of(invoice));
        given(securityContextService.hasAnyRole("ADMIN", "STAFF")).willReturn(true);
        given(invoiceLineRepository.findByInvoiceIdOrderByLineOrderAsc(invoiceId)).willReturn(List.of(line));

        // WHEN
        InvoiceResponse response = invoiceService.getInvoice(invoiceId);

        // THEN
        assertThat(response.petId()).isNull();
        assertThat(response.lines()).hasSize(1);
        assertThat(response.lines().get(0).serviceOrderId()).isNull();
        assertThat(response.lines().get(0).medicineId()).isNull();
        assertThat(response.lines().get(0).quantity()).isNull();
    }

    @Test
    void should_GetInvoice_WithNonNullFields_Success() {
        // GIVEN
        UUID invoiceId = UUID.randomUUID();
        Invoice invoice = new Invoice();
        invoice.setId(invoiceId);
        Users owner = new Users();
        owner.setId(UUID.randomUUID());
        invoice.setOwner(owner);
        
        com.astral.express.pccms.pet.entity.Pets pet = new com.astral.express.pccms.pet.entity.Pets();
        pet.setId(UUID.randomUUID());
        invoice.setPet(pet);
        
        InvoiceLine line = new InvoiceLine();
        line.setId(UUID.randomUUID());
        line.setInvoice(invoice);
        
        com.astral.express.pccms.appointment.entity.ServiceOrder serviceOrder = new com.astral.express.pccms.appointment.entity.ServiceOrder();
        serviceOrder.setId(UUID.randomUUID());
        line.setServiceOrder(serviceOrder);
        
        com.astral.express.pccms.medicine.entity.Medicine medicine = new com.astral.express.pccms.medicine.entity.Medicine();
        medicine.setId(UUID.randomUUID());
        line.setMedicine(medicine);
        
        line.setQuantity(new java.math.BigDecimal("2.5"));
        
        given(invoiceRepository.findById(invoiceId)).willReturn(Optional.of(invoice));
        given(securityContextService.hasAnyRole("ADMIN", "STAFF")).willReturn(true);
        given(invoiceLineRepository.findByInvoiceIdOrderByLineOrderAsc(invoiceId)).willReturn(List.of(line));

        // WHEN
        InvoiceResponse response = invoiceService.getInvoice(invoiceId);

        // THEN
        assertThat(response.petId()).isEqualTo(pet.getId());
        assertThat(response.lines()).hasSize(1);
        assertThat(response.lines().get(0).serviceOrderId()).isEqualTo(serviceOrder.getId());
        assertThat(response.lines().get(0).medicineId()).isEqualTo(medicine.getId());
        assertThat(response.lines().get(0).quantity()).isEqualTo(2); // intValue
    }

}