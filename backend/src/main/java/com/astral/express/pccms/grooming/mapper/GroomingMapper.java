package com.astral.express.pccms.grooming.mapper;

import com.astral.express.pccms.billing.entity.Invoice;
import com.astral.express.pccms.boarding.entity.ServiceCatalog;
import com.astral.express.pccms.grooming.dto.response.GroomingInvoiceSummaryResponse;
import com.astral.express.pccms.grooming.dto.response.GroomingServiceResponse;
import com.astral.express.pccms.grooming.dto.response.GroomingStationResponse;
import com.astral.express.pccms.grooming.dto.response.GroomingTicketResponse;
import com.astral.express.pccms.grooming.entity.Appointment;
import com.astral.express.pccms.grooming.entity.GroomingStation;
import com.astral.express.pccms.grooming.entity.GroomingTicket;
import com.astral.express.pccms.user.entity.Users;
import org.springframework.stereotype.Component;

@Component
public class GroomingMapper {

    public GroomingTicketResponse toTicketResponse(GroomingTicket ticket, Invoice invoice) {
        Appointment appointment = ticket.getAppointment();
        ServiceCatalog service = appointment.getServiceOrder().getService();
        GroomingStation station = ticket.getStation();
        Users staff = ticket.getAssignedStaff();
        return new GroomingTicketResponse(
                ticket.getId(),
                appointment.getId(),
                appointment.getServiceOrder().getId(),
                appointment.getServiceOrder().getOrderCode(),
                appointment.getServiceOrder().getStatusCode(),
                appointment.getServiceOrder().getOwner().getId(),
                appointment.getServiceOrder().getOwner().getFullName(),
                appointment.getServiceOrder().getPet().getId(),
                appointment.getServiceOrder().getPet().getName(),
                service.getId(),
                service.getServiceCode(),
                service.getName(),
                service.getBasePriceVnd(),
                service.getDurationMinutes(),
                appointment.getScheduledStartAt(),
                appointment.getScheduledEndAt(),
                appointment.getStatusCode(),
                ticket.getStatusCode(),
                station == null ? null : station.getId(),
                station == null ? null : station.getStationCode(),
                station == null ? null : station.getName(),
                staff == null ? null : staff.getId(),
                staff == null ? null : staff.getFullName(),
                ticket.getStartedAt(),
                ticket.getCompletedAt(),
                ticket.getOwnerNote(),
                ticket.getInternalNote(),
                appointment.getServiceOrder().getBaseAmountVnd(),
                appointment.getServiceOrder().getFinalAmountVnd(),
                toInvoiceSummary(invoice));
    }

    public GroomingServiceResponse toServiceResponse(ServiceCatalog service) {
        return new GroomingServiceResponse(
                service.getId(),
                service.getServiceCode(),
                service.getName(),
                service.getDescription(),
                service.getBasePriceVnd(),
                service.getDurationMinutes(),
                service.getIsActive());
    }

    public GroomingStationResponse toStationResponse(GroomingStation station) {
        return new GroomingStationResponse(
                station.getId(),
                station.getStationCode(),
                station.getName(),
                station.getIsActive());
    }

    private GroomingInvoiceSummaryResponse toInvoiceSummary(Invoice invoice) {
        if (invoice == null) {
            return null;
        }
        return new GroomingInvoiceSummaryResponse(
                invoice.getId(),
                invoice.getInvoiceCode(),
                invoice.getStatusCode(),
                invoice.getTotalAmountVnd(),
                invoice.getPaidAmountVnd(),
                invoice.getIssuedAt());
    }
}
