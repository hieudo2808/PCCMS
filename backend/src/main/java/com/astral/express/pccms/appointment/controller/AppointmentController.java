package com.astral.express.pccms.appointment.controller;

import com.astral.express.pccms.appointment.dto.request.CreateBoardingBookingRequest;
import com.astral.express.pccms.appointment.dto.request.CreateGroomingAppointmentRequest;
import com.astral.express.pccms.appointment.dto.request.CreateMedicalAppointmentRequest;
import com.astral.express.pccms.appointment.dto.request.QuickCheckInRequest;
import com.astral.express.pccms.appointment.dto.request.UpdateGroomingStatusRequest;
import com.astral.express.pccms.appointment.dto.response.AppointmentResponse;
import com.astral.express.pccms.appointment.dto.response.AvailabilitySummaryResponse;
import com.astral.express.pccms.appointment.dto.response.BoardingBookingResponse;
import com.astral.express.pccms.appointment.dto.response.CustomerLookupResponse;
import com.astral.express.pccms.appointment.dto.response.GroomingBoardCardResponse;
import com.astral.express.pccms.appointment.dto.response.QueueEntryResponse;
import com.astral.express.pccms.appointment.dto.response.RoomTypeOptionResponse;
import com.astral.express.pccms.appointment.dto.response.ServiceCatalogOptionResponse;
import com.astral.express.pccms.appointment.dto.response.TimeSlotResponse;
import com.astral.express.pccms.appointment.dto.response.VetOptionResponse;
import com.astral.express.pccms.appointment.entity.Appointment;
import com.astral.express.pccms.appointment.entity.AppointmentStatus;
import com.astral.express.pccms.appointment.entity.ServiceCategory;
import com.astral.express.pccms.appointment.service.AppointmentAvailabilityUseCase;
import com.astral.express.pccms.appointment.service.AppointmentLifecycleUseCase;
import com.astral.express.pccms.appointment.service.AppointmentQueryUseCase;
import com.astral.express.pccms.appointment.service.AppointmentResponseAssembler;
import com.astral.express.pccms.appointment.service.BoardingBookingUseCase;
import com.astral.express.pccms.appointment.service.CreateMedicalAppointmentUseCase;
import com.astral.express.pccms.appointment.service.GroomingBookingUseCase;
import com.astral.express.pccms.appointment.service.QuickCheckInUseCase;
import com.astral.express.pccms.common.dto.ApiResponse;
import com.astral.express.pccms.common.dto.PageResponse;
import com.astral.express.pccms.identity.security.SecurityContextService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentAvailabilityUseCase availabilityUseCase;
    private final AppointmentLifecycleUseCase lifecycleUseCase;
    private final AppointmentQueryUseCase queryUseCase;
    private final BoardingBookingUseCase boardingBookingUseCase;
    private final GroomingBookingUseCase groomingBookingUseCase;
    private final CreateMedicalAppointmentUseCase createMedicalAppointmentUseCase;
    private final QuickCheckInUseCase quickCheckInUseCase;
    private final AppointmentResponseAssembler appointmentResponseAssembler;
    private final SecurityContextService SecurityContextService;

    @PostMapping
    @PreAuthorize("hasAuthority('APPOINTMENT_CREATE')")
    public ResponseEntity<ApiResponse<AppointmentResponse>> createMedicalAppointment(
            @Valid @RequestBody CreateMedicalAppointmentRequest request) {
        UUID ownerId = SecurityContextService.getCurrentUserId();
        Appointment appointment = createMedicalAppointmentUseCase.createMedicalAppointment(request, ownerId);
        AppointmentResponse response = appointmentResponseAssembler.toResponse(appointment, null);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, 201, "Đặt lịch thành công", response));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('APPOINTMENT_CREATE')")
    public ApiResponse<PageResponse<AppointmentResponse>> listOwnerAppointments(
            @PageableDefault(size = 20, sort = "scheduledStartAt", direction = Sort.Direction.DESC) Pageable pageable) {
        UUID ownerId = SecurityContextService.getCurrentUserId();
        return ApiResponse.success(queryUseCase.listOwnerAppointments(ownerId, pageable));
    }

    @GetMapping("/today")
    @PreAuthorize("hasAuthority('APPOINTMENT_RECEIVE') or hasRole('VETERINARIAN')")
    public ApiResponse<List<AppointmentResponse>> listTodayAppointments(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) AppointmentStatus status,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String customerName) {
        return ApiResponse.success(queryUseCase.listTodayAppointments(date, status, phone, customerName));
    }

    @GetMapping("/slots")
    @PreAuthorize("hasAuthority('APPOINTMENT_READ')")
    public ApiResponse<List<TimeSlotResponse>> getAvailableSlots(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) UUID vetId) {
        return ApiResponse.success(availabilityUseCase.getAvailableSlots(date, vetId));
    }

    @GetMapping("/vets")
    @PreAuthorize("hasAuthority('APPOINTMENT_READ')")
    public ApiResponse<List<VetOptionResponse>> listAvailableVets(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime slotStart) {
        return ApiResponse.success(availabilityUseCase.listAvailableVets(date, slotStart));
    }

    @GetMapping("/vets/on-duty")
    @PreAuthorize("hasAnyAuthority('APPOINTMENT_READ', 'APPOINTMENT_RECEIVE')")
    public ApiResponse<List<VetOptionResponse>> listVetsOnDuty(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.success(availabilityUseCase.listVetsOnDuty(date));
    }

    @GetMapping("/availability")
    @PreAuthorize("hasAnyAuthority('APPOINTMENT_READ', 'APPOINTMENT_RECEIVE')")
    public ApiResponse<AvailabilitySummaryResponse> getAvailabilitySummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime slotStart) {
        return ApiResponse.success(availabilityUseCase.getAvailabilitySummary(date, slotStart));
    }

    @PostMapping("/{appointmentId}/check-in")
    @PreAuthorize("hasAuthority('APPOINTMENT_RECEIVE')")
    public ApiResponse<AppointmentResponse> checkIn(@PathVariable UUID appointmentId) {
        UUID staffId = SecurityContextService.getCurrentUserId();
        return ApiResponse.success(lifecycleUseCase.checkIn(appointmentId, staffId), "Tiếp nhận thành công");
    }

    @PostMapping("/{appointmentId}/start-exam")
    @PreAuthorize("hasRole('VETERINARIAN')")
    public ApiResponse<AppointmentResponse> startExam(@PathVariable UUID appointmentId) {
        UUID vetId = SecurityContextService.getCurrentUserId();
        return ApiResponse.success(lifecycleUseCase.startExam(appointmentId, vetId), "Bắt đầu khám thành công");
    }

    @PostMapping("/{appointmentId}/cancel")
    @PreAuthorize("hasAuthority('APPOINTMENT_CREATE') or hasAuthority('APPOINTMENT_RECEIVE')")
    public ApiResponse<AppointmentResponse> cancel(@PathVariable UUID appointmentId) {
        UUID actorId = SecurityContextService.getCurrentUserId();
        boolean isStaff = SecurityContextService.isAdminOrStaff();
        return ApiResponse.success(lifecycleUseCase.cancel(appointmentId, actorId, isStaff), "Hủy lịch thành công");
    }

    @PostMapping("/quick-check-in")
    @PreAuthorize("hasAuthority('APPOINTMENT_RECEIVE')")
    public ApiResponse<AppointmentResponse> quickCheckIn(@Valid @RequestBody QuickCheckInRequest request) {
        UUID staffId = SecurityContextService.getCurrentUserId();
        QuickCheckInUseCase.Result result = quickCheckInUseCase.execute(request, staffId);
        return ApiResponse.success(appointmentResponseAssembler.toResponse(result.appointment(), result.queueNumber()), "Tiếp nhận thành công");
    }

    @GetMapping("/queue")
    @PreAuthorize("hasRole('VETERINARIAN')")
    public ApiResponse<List<QueueEntryResponse>> getVetQueue(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        UUID vetId = SecurityContextService.getCurrentUserId();
        return ApiResponse.success(queryUseCase.getVetQueue(vetId, date));
    }

    @GetMapping("/customer-lookup")
    @PreAuthorize("hasAuthority('APPOINTMENT_RECEIVE')")
    public ApiResponse<CustomerLookupResponse> lookupCustomer(@RequestParam String phone) {
        return ApiResponse.success(queryUseCase.lookupCustomerByPhone(phone));
    }

    @PostMapping("/grooming")
    @PreAuthorize("hasAuthority('GROOMING_CREATE')")
    public ResponseEntity<ApiResponse<AppointmentResponse>> createGroomingAppointment(
            @Valid @RequestBody CreateGroomingAppointmentRequest request) {
        UUID ownerId = SecurityContextService.getCurrentUserId();
        AppointmentResponse response = groomingBookingUseCase.createGroomingAppointment(request, ownerId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, 201, "Đặt lịch spa thành công", response));
    }

    @GetMapping("/grooming/board")
    @PreAuthorize("hasAuthority('GROOMING_READ')")
    public ApiResponse<List<GroomingBoardCardResponse>> listGroomingBoard(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.success(groomingBookingUseCase.listGroomingBoard(date));
    }

    @PatchMapping("/grooming/{ticketId}/status")
    @PreAuthorize("hasAuthority('GROOMING_UPDATE')")
    public ApiResponse<GroomingBoardCardResponse> updateGroomingStatus(
            @PathVariable UUID ticketId,
            @Valid @RequestBody UpdateGroomingStatusRequest request) {
        return ApiResponse.success(groomingBookingUseCase.updateGroomingStatus(ticketId, request));
    }

    @PostMapping("/boarding")
    @PreAuthorize("hasAuthority('BOARDING_CREATE')")
    public ResponseEntity<ApiResponse<BoardingBookingResponse>> createBoardingBooking(
            @Valid @RequestBody CreateBoardingBookingRequest request) {
        UUID ownerId = SecurityContextService.getCurrentUserId();
        BoardingBookingResponse response = boardingBookingUseCase.createBoardingBooking(request, ownerId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, 201, "Đặt phòng lưu trú thành công", response));
    }

    @GetMapping("/boarding")
    @PreAuthorize("hasAuthority('BOARDING_READ')")
    public ApiResponse<List<BoardingBookingResponse>> listOwnerBoardingBookings() {
        UUID ownerId = SecurityContextService.getCurrentUserId();
        return ApiResponse.success(boardingBookingUseCase.listOwnerBoardingBookings(ownerId));
    }

    @GetMapping("/room-types")
    @PreAuthorize("hasAuthority('BOARDING_CREATE')")
    public ApiResponse<List<RoomTypeOptionResponse>> listRoomTypes() {
        return ApiResponse.success(boardingBookingUseCase.listActiveRoomTypes());
    }

    @GetMapping("/services")
    @PreAuthorize("hasAnyAuthority('APPOINTMENT_READ', 'GROOMING_CREATE', 'BOARDING_CREATE')")
    public ApiResponse<List<ServiceCatalogOptionResponse>> listServices(
            @RequestParam ServiceCategory category) {
        return ApiResponse.success(queryUseCase.listServicesByCategory(category));
    }
}
