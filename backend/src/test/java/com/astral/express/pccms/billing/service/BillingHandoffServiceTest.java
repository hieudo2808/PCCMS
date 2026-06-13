package com.astral.express.pccms.billing.service;

import com.astral.express.pccms.billing.entity.Invoice;
import com.astral.express.pccms.billing.entity.InvoiceLine;
import com.astral.express.pccms.billing.repository.InvoiceLineRepository;
import com.astral.express.pccms.billing.repository.InvoiceRepository;
import com.astral.express.pccms.boarding.entity.BoardingBooking;
import com.astral.express.pccms.boarding.entity.BoardingSession;
import com.astral.express.pccms.appointment.entity.ServiceOrder;
import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.pet.entity.Pets;
import com.astral.express.pccms.room.entity.RoomType;
import com.astral.express.pccms.user.entity.Users;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BillingHandoffServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private InvoiceLineRepository invoiceLineRepository;

    @InjectMocks
    private BillingHandoffService billingHandoffService;

    @ParameterizedTest(name = "[{0}] {1}")
    @CsvFileSource(resources = "/testcases/boarding-billing-handoff.csv", numLinesToSkip = 1)
    void should_create_boarding_invoice_when_checkout_finishes(
            String ruleId,
            String caseId,
            int durationHours,
            Long unitPrice,
            boolean invoiceExists,
            String expectedResult,
            Long expectedTotal) {
        UUID serviceOrderId = UUID.randomUUID();
        OffsetDateTime startAt = OffsetDateTime.parse("2026-06-01T09:00:00+07:00");
        OffsetDateTime endAt = startAt.plusHours(durationHours);
        Users owner = Users.builder().id(UUID.randomUUID()).fullName("Owner").build();
        Users staff = Users.builder().id(UUID.randomUUID()).fullName("Staff").build();
        Pets pet = Pets.builder().id(UUID.randomUUID()).name("Milu").owner(owner).build();
        RoomType roomType = RoomType.builder()
                .id(UUID.randomUUID())
                .name("Standard")
                .baseDailyPriceVnd(unitPrice)
                .build();
        ServiceOrder serviceOrder = ServiceOrder.builder()
                .id(serviceOrderId)
                .orderCode("SO-001")
                .owner(owner)
                .pet(pet)
                .build();
        BoardingBooking booking = BoardingBooking.builder()
                .id(UUID.randomUUID())
                .bookingCode("BRD-001")
                .owner(owner)
                .pet(pet)
                .requestedRoomType(roomType)
                .expectedCheckinAt(startAt)
                .expectedCheckoutAt(endAt)
                .serviceOrder(serviceOrder)
                .build();
        BoardingSession session = BoardingSession.builder()
                .id(UUID.randomUUID())
                .booking(booking)
                .actualCheckinAt(startAt)
                .actualCheckoutAt(endAt)
                .build();

        given(invoiceRepository.existsByServiceOrderId(serviceOrderId)).willReturn(invoiceExists);

        if ("EXCEPTION".equals(expectedResult)) {
            assertThatThrownBy(() -> billingHandoffService.createBoardingInvoice(booking, session, staff))
                    .isInstanceOf(BusinessException.class);
            verify(invoiceRepository, never()).save(any(Invoice.class));
            verify(invoiceLineRepository, never()).save(any(InvoiceLine.class));
            return;
        }

        given(invoiceRepository.save(any(Invoice.class))).willAnswer(invocation -> invocation.getArgument(0));
        ArgumentCaptor<Invoice> invoiceCaptor = ArgumentCaptor.forClass(Invoice.class);
        ArgumentCaptor<InvoiceLine> lineCaptor = ArgumentCaptor.forClass(InvoiceLine.class);

        Invoice invoice = billingHandoffService.createBoardingInvoice(booking, session, staff);

        verify(invoiceRepository).save(invoiceCaptor.capture());
        verify(invoiceLineRepository).save(lineCaptor.capture());
        assertThat(invoice.getTotalAmountVnd()).isEqualTo(expectedTotal);
        assertThat(invoiceCaptor.getValue().getTotalAmountVnd()).isEqualTo(expectedTotal);
        assertThat(lineCaptor.getValue().getQuantity()).isEqualByComparingTo(BigDecimal.valueOf(expectedTotal / unitPrice));
        assertThat(lineCaptor.getValue().getUnitPriceVnd()).isEqualTo(unitPrice);
    }

    @Test
    void should_create_grooming_invoice_when_not_exists() {
        UUID serviceOrderId = UUID.randomUUID();
        Users owner = Users.builder().id(UUID.randomUUID()).fullName("Owner").build();
        Users staff = Users.builder().id(UUID.randomUUID()).fullName("Staff").build();
        Pets pet = Pets.builder().id(UUID.randomUUID()).name("Milu").owner(owner).build();
        
        com.astral.express.pccms.appointment.entity.ServiceCatalog service = new com.astral.express.pccms.appointment.entity.ServiceCatalog();
        service.setName("Grooming Basic");
        service.setBasePriceVnd(250000L);
        
        ServiceOrder serviceOrder = ServiceOrder.builder()
                .id(serviceOrderId)
                .orderCode("SO-002")
                .owner(owner)
                .pet(pet)
                .service(service)
                .build();
                
        com.astral.express.pccms.appointment.entity.Appointment appt = new com.astral.express.pccms.appointment.entity.Appointment();
        appt.setScheduledStartAt(OffsetDateTime.parse("2026-06-01T09:00:00+07:00"));
        appt.setScheduledEndAt(OffsetDateTime.parse("2026-06-01T10:00:00+07:00"));
        
        com.astral.express.pccms.appointment.entity.GroomingTicket ticket = new com.astral.express.pccms.appointment.entity.GroomingTicket();
        com.astral.express.pccms.grooming.entity.GroomingStation station = new com.astral.express.pccms.grooming.entity.GroomingStation();
        station.setName("Station 1");
        ticket.setStation(station);

        given(invoiceRepository.findByServiceOrderId(serviceOrderId)).willReturn(java.util.Optional.empty());
        given(invoiceRepository.save(any(Invoice.class))).willAnswer(invocation -> invocation.getArgument(0));

        Invoice invoice = billingHandoffService.createGroomingInvoice(serviceOrder, appt, ticket, staff);

        assertThat(invoice.getTotalAmountVnd()).isEqualTo(250000L);
        verify(invoiceLineRepository).save(any(InvoiceLine.class));
    }

    @Test
    void should_return_existing_grooming_invoice() {
        UUID serviceOrderId = UUID.randomUUID();
        ServiceOrder serviceOrder = ServiceOrder.builder().id(serviceOrderId).build();
        Invoice existingInvoice = Invoice.builder().id(UUID.randomUUID()).build();
        
        given(invoiceRepository.findByServiceOrderId(serviceOrderId)).willReturn(java.util.Optional.of(existingInvoice));
        
        Invoice invoice = billingHandoffService.createGroomingInvoice(serviceOrder, null, null, null);
        
        assertThat(invoice.getId()).isEqualTo(existingInvoice.getId());
        verify(invoiceRepository, never()).save(any(Invoice.class));
    }

    @Test
    void should_resolve_expected_checkin_when_actual_is_null() {
        UUID serviceOrderId = UUID.randomUUID();
        OffsetDateTime startAt = OffsetDateTime.parse("2026-06-01T09:00:00+07:00");
        OffsetDateTime endAt = startAt.plusHours(24); // 1 day
        Users owner = Users.builder().id(UUID.randomUUID()).fullName("Owner").build();
        Users staff = Users.builder().id(UUID.randomUUID()).fullName("Staff").build();
        Pets pet = Pets.builder().id(UUID.randomUUID()).name("Milu").owner(owner).build();
        RoomType roomType = RoomType.builder().id(UUID.randomUUID()).name("Standard").baseDailyPriceVnd(100000L).build();
        
        ServiceOrder serviceOrder = ServiceOrder.builder().id(serviceOrderId).orderCode("SO-001").owner(owner).pet(pet).build();
        BoardingBooking booking = BoardingBooking.builder()
                .id(UUID.randomUUID()).bookingCode("BRD-001").owner(owner).pet(pet)
                .requestedRoomType(roomType)
                .expectedCheckinAt(startAt) // Use expected
                .expectedCheckoutAt(endAt) // Use expected
                .serviceOrder(serviceOrder).build();
                
        BoardingSession session = BoardingSession.builder()
                .id(UUID.randomUUID()).booking(booking)
                .actualCheckinAt(null) // Null actual
                .actualCheckoutAt(null) // Null actual
                .build();

        given(invoiceRepository.existsByServiceOrderId(serviceOrderId)).willReturn(false);
        given(invoiceRepository.save(any(Invoice.class))).willAnswer(invocation -> invocation.getArgument(0));

        Invoice invoice = billingHandoffService.createBoardingInvoice(booking, session, staff);

        assertThat(invoice.getTotalAmountVnd()).isEqualTo(100000L); // 1 day
    }
}


