package com.astral.express.pccms.appointment.service;

import com.astral.express.pccms.appointment.dto.request.CreateBoardingBookingRequest;
import com.astral.express.pccms.appointment.dto.request.CreateGroomingAppointmentRequest;
import com.astral.express.pccms.appointment.dto.request.CreateMedicalAppointmentRequest;
import com.astral.express.pccms.appointment.dto.request.QuickCheckInRequest;
import com.astral.express.pccms.appointment.dto.request.UpdateGroomingStatusRequest;
import com.astral.express.pccms.appointment.dto.response.AppointmentResponse;
import com.astral.express.pccms.appointment.dto.response.AvailabilitySummaryResponse;
import com.astral.express.pccms.appointment.dto.response.BoardingBookingResponse;
import com.astral.express.pccms.appointment.dto.response.GroomingBoardCardResponse;
import com.astral.express.pccms.appointment.dto.response.QueueEntryResponse;
import com.astral.express.pccms.appointment.dto.response.RoomTypeOptionResponse;
import com.astral.express.pccms.appointment.dto.response.ServiceCatalogOptionResponse;
import com.astral.express.pccms.appointment.dto.response.TimeSlotResponse;
import com.astral.express.pccms.appointment.dto.response.VetOptionResponse;
import com.astral.express.pccms.appointment.entity.AppointmentStatus;
import com.astral.express.pccms.appointment.entity.GroomingStatus;
import com.astral.express.pccms.appointment.entity.ServiceCategory;
import com.astral.express.pccms.common.dto.PageResponse;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public interface AppointmentService {

    AppointmentResponse createMedicalAppointment(CreateMedicalAppointmentRequest request, UUID ownerId);

    PageResponse<AppointmentResponse> listOwnerAppointments(UUID ownerId, Pageable pageable);

    List<AppointmentResponse> listTodayAppointments(
            LocalDate date, AppointmentStatus status, String phone, String customerName);

    List<TimeSlotResponse> getAvailableSlots(LocalDate date, UUID vetId);

    List<VetOptionResponse> listAvailableVets(LocalDate date, LocalTime slotStart);

    List<VetOptionResponse> listVetsOnDuty(LocalDate date);

    AvailabilitySummaryResponse getAvailabilitySummary(LocalDate date, LocalTime slotStart);

    AppointmentResponse checkIn(UUID appointmentId, UUID staffId);

    AppointmentResponse cancel(UUID appointmentId, UUID actorId, boolean isStaff);

    AppointmentResponse quickCheckIn(QuickCheckInRequest request, UUID staffId);

    List<QueueEntryResponse> getVetQueue(UUID vetId, LocalDate date);

    com.astral.express.pccms.appointment.dto.response.CustomerLookupResponse lookupCustomerByPhone(String phone);

    AppointmentResponse createGroomingAppointment(CreateGroomingAppointmentRequest request, UUID ownerId);

    List<GroomingBoardCardResponse> listGroomingBoard(LocalDate date);

    GroomingBoardCardResponse updateGroomingStatus(UUID ticketId, UpdateGroomingStatusRequest request);

    BoardingBookingResponse createBoardingBooking(CreateBoardingBookingRequest request, UUID ownerId);

    List<BoardingBookingResponse> listOwnerBoardingBookings(UUID ownerId);

    List<RoomTypeOptionResponse> listActiveRoomTypes();

    List<ServiceCatalogOptionResponse> listServicesByCategory(ServiceCategory category);
}
