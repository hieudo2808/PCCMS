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
}


