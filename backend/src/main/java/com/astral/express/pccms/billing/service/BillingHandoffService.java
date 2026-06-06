package com.astral.express.pccms.billing.service;

import com.astral.express.pccms.billing.entity.Invoice;
import com.astral.express.pccms.boarding.entity.BoardingBooking;
import com.astral.express.pccms.boarding.entity.BoardingSession;
import com.astral.express.pccms.boarding.entity.ServiceOrder;
import com.astral.express.pccms.grooming.entity.Appointment;
import com.astral.express.pccms.grooming.entity.GroomingTicket;
import com.astral.express.pccms.user.entity.Users;

public interface BillingHandoffService {
    Invoice createBoardingInvoice(BoardingBooking booking, BoardingSession session, Users createdBy);

    Invoice createGroomingInvoice(ServiceOrder serviceOrder, Appointment appointment, GroomingTicket ticket, Users createdBy);
}
