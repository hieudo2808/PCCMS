package com.astral.express.pccms.billing.service.impl;

import com.astral.express.pccms.billing.entity.Invoice;
import com.astral.express.pccms.billing.entity.InvoiceLine;
import com.astral.express.pccms.billing.entity.InvoiceStatus;
import com.astral.express.pccms.billing.repository.InvoiceLineRepository;
import com.astral.express.pccms.billing.repository.InvoiceRepository;
import com.astral.express.pccms.billing.service.BillingHandoffService;
import com.astral.express.pccms.boarding.entity.BoardingBooking;
import com.astral.express.pccms.boarding.entity.BoardingSession;
import com.astral.express.pccms.boarding.entity.ServiceOrder;
import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.grooming.entity.Appointment;
import com.astral.express.pccms.grooming.entity.GroomingTicket;
import com.astral.express.pccms.user.entity.Users;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BillingHandoffServiceImpl implements BillingHandoffService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceLineRepository invoiceLineRepository;

    @Override
    @Transactional
    public Invoice createBoardingInvoice(BoardingBooking booking, BoardingSession session, Users createdBy) {
        if (invoiceRepository.existsByServiceOrderId(booking.getServiceOrder().getId())) {
            throw new BusinessException(ErrorCode.ERR_BILLING_001_INVOICE_ALREADY_EXISTS);
        }

        BigDecimal billableDays = BigDecimal.valueOf(calculateBillableDays(
                resolveStartTime(booking, session),
                resolveEndTime(booking, session)));
        BigDecimal unitPrice = booking.getRequestedRoomType().getBaseDailyPriceVnd();
        BigDecimal totalAmount = billableDays.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP);

        Invoice invoice = Invoice.builder()
                .invoiceCode(generateInvoiceCode())
                .owner(booking.getOwner())
                .pet(booking.getPet())
                .statusCode(InvoiceStatus.UNPAID)
                .totalAmountVnd(totalAmount)
                .createdBy(createdBy)
                .note("Boarding invoice for booking " + booking.getBookingCode())
                .build();
        Invoice savedInvoice = invoiceRepository.save(invoice);

        InvoiceLine line = InvoiceLine.builder()
                .invoice(savedInvoice)
                .serviceOrder(booking.getServiceOrder())
                .description(buildBoardingDescription(booking, session))
                .quantity(billableDays)
                .unitPriceVnd(unitPrice)
                .lineOrder(1)
                .build();
        invoiceLineRepository.save(line);
        return savedInvoice;
    }

    @Override
    @Transactional
    public Invoice createGroomingInvoice(ServiceOrder serviceOrder, Appointment appointment, GroomingTicket ticket, Users createdBy) {
        return invoiceRepository.findByServiceOrderId(serviceOrder.getId())
                .orElseGet(() -> createNewGroomingInvoice(serviceOrder, appointment, ticket, createdBy));
    }

    private Invoice createNewGroomingInvoice(ServiceOrder serviceOrder, Appointment appointment, GroomingTicket ticket, Users createdBy) {
        BigDecimal unitPrice = serviceOrder.getService().getBasePriceVnd();
        BigDecimal totalAmount = unitPrice.setScale(2, RoundingMode.HALF_UP);

        Invoice invoice = Invoice.builder()
                .invoiceCode(generateInvoiceCode())
                .owner(serviceOrder.getOwner())
                .pet(serviceOrder.getPet())
                .statusCode(InvoiceStatus.UNPAID)
                .totalAmountVnd(totalAmount)
                .createdBy(createdBy)
                .note("Grooming invoice for order " + serviceOrder.getOrderCode())
                .build();
        Invoice savedInvoice = invoiceRepository.save(invoice);

        InvoiceLine line = InvoiceLine.builder()
                .invoice(savedInvoice)
                .serviceOrder(serviceOrder)
                .description(buildGroomingDescription(serviceOrder, appointment, ticket))
                .quantity(BigDecimal.ONE.setScale(2, RoundingMode.HALF_UP))
                .unitPriceVnd(unitPrice)
                .lineOrder(1)
                .build();
        invoiceLineRepository.save(line);
        return savedInvoice;
    }

    private OffsetDateTime resolveStartTime(BoardingBooking booking, BoardingSession session) {
        if (session.getActualCheckinAt() != null) {
            return session.getActualCheckinAt();
        }
        return booking.getExpectedCheckinAt();
    }

    private OffsetDateTime resolveEndTime(BoardingBooking booking, BoardingSession session) {
        if (session.getActualCheckoutAt() != null) {
            return session.getActualCheckoutAt();
        }
        return booking.getExpectedCheckoutAt();
    }

    private long calculateBillableDays(OffsetDateTime startAt, OffsetDateTime endAt) {
        long minutes = Duration.between(startAt, endAt).toMinutes();
        if (minutes <= 0) {
            return 1;
        }
        long oneDayMinutes = 24L * 60L;
        return Math.max(1, (minutes + oneDayMinutes - 1) / oneDayMinutes);
    }

    private String buildBoardingDescription(BoardingBooking booking, BoardingSession session) {
        String range = resolveStartTime(booking, session).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                + " - "
                + resolveEndTime(booking, session).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        return "Boarding stay for " + booking.getPet().getName()
                + " (" + booking.getRequestedRoomType().getName() + "), " + range;
    }

    private String buildGroomingDescription(ServiceOrder serviceOrder, Appointment appointment, GroomingTicket ticket) {
        String range = appointment.getScheduledStartAt().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                + " - "
                + appointment.getScheduledEndAt().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        String stationName = ticket.getStation() == null ? "unassigned station" : ticket.getStation().getName();
        return "Grooming service for " + serviceOrder.getPet().getName()
                + " (" + serviceOrder.getService().getName() + ", " + stationName + "), " + range;
    }

    private String generateInvoiceCode() {
        return "INV-" + OffsetDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
    }
}
