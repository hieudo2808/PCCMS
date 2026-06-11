package com.astral.express.pccms.appointment.service;

import com.astral.express.pccms.appointment.dto.response.AppointmentResponse;
import com.astral.express.pccms.appointment.dto.response.CustomerLookupResponse;
import com.astral.express.pccms.appointment.dto.response.QueueEntryResponse;
import com.astral.express.pccms.appointment.dto.response.ServiceCatalogOptionResponse;
import com.astral.express.pccms.appointment.entity.Appointment;
import com.astral.express.pccms.appointment.entity.AppointmentStatus;
import com.astral.express.pccms.appointment.entity.ServiceCategory;
import com.astral.express.pccms.appointment.repository.AppointmentRepository;
import com.astral.express.pccms.appointment.repository.ServiceCatalogRepository;
import com.astral.express.pccms.common.dto.PageResponse;
import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.pet.repository.PetRepository;
import com.astral.express.pccms.user.entity.Users;
import com.astral.express.pccms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AppointmentQueryUseCase {

    private final AppointmentRepository appointmentRepository;
    private final ServiceCatalogRepository serviceCatalogRepository;
    private final PetRepository petRepository;
    private final UserRepository userRepository;
    private final AppointmentResponseAssembler assembler;
    private final AppointmentReceptionService receptionService;

    @Transactional(readOnly = true)
    public AppointmentResponse getAppointmentById(UUID appointmentId) {
        Appointment appointment = appointmentRepository.findDetailById(appointmentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_APT_001_NOT_FOUND));
        return assembler.toResponse(appointment, receptionService.getQueueNumberForAppointment(appointmentId));
    }

    @Transactional(readOnly = true)
    public PageResponse<AppointmentResponse> listOwnerAppointments(UUID ownerId, Pageable pageable) {
        Page<Appointment> page = appointmentRepository.findByOwnerId(ownerId, pageable);
        return PageResponse.of(page.map(a -> assembler.toResponse(a, receptionService.getQueueNumberForAppointment(a.getId()))));
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> listTodayAppointments(
            LocalDate date, AppointmentStatus status, String phone, String customerName) {
        LocalDate targetDate = date != null ? date : ClinicDateTime.today();
        OffsetDateTime dayStart = ClinicDateTime.startOfDay(targetDate);
        OffsetDateTime dayEnd = ClinicDateTime.endOfDay(targetDate);

        String phoneNeedle = phone != null && !phone.isBlank() ? normalizePhone(phone) : null;
        String nameNeedle = customerName != null && !customerName.isBlank()
                ? customerName.trim().toLowerCase()
                : null;

        return appointmentRepository.findAppointmentsForDay(dayStart, dayEnd).stream()
                .filter(a -> status == null || a.getStatusCode() == status)
                .filter(a -> phoneNeedle == null || matchesPhone(a.getServiceOrder().getOwner().getPhone(), phoneNeedle))
                .filter(a -> nameNeedle == null || containsIgnoreCase(a.getServiceOrder().getOwner().getFullName(), nameNeedle))
                .map(a -> assembler.toResponse(a, receptionService.getQueueNumberForAppointment(a.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public CustomerLookupResponse lookupCustomerByPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            throw new BusinessException(ErrorCode.ERR_APT_008_PHONE_REQUIRED);
        }
        Users owner = userRepository.findByNormalizedPhone(normalizePhone(phone))
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_ACC_002_USER_NOT_FOUND));

        List<CustomerLookupResponse.PetSummary> pets = petRepository
                .findByOwner_IdAndIsActive(owner.getId(), true, PageRequest.of(0, 100))
                .getContent()
                .stream()
                .map(p -> new CustomerLookupResponse.PetSummary(p.getId(), p.getName()))
                .toList();

        return new CustomerLookupResponse(owner.getId(), owner.getFullName(), owner.getPhone(), pets);
    }

    @Transactional(readOnly = true)
    public List<QueueEntryResponse> getVetQueue(UUID vetId, LocalDate date) {
        LocalDate targetDate = date != null ? date : ClinicDateTime.today();
        OffsetDateTime dayStart = ClinicDateTime.startOfDay(targetDate);
        OffsetDateTime dayEnd = ClinicDateTime.endOfDay(targetDate);

        return receptionService.getQueueForVet(vetId, dayStart, dayEnd).stream()
                .map(rt -> {
                    Appointment a = rt.getAppointment();
                    return new QueueEntryResponse(
                            rt.getQueueNumber(),
                            a.getId(),
                            a.getServiceOrder().getPet().getId(),
                            a.getServiceOrder().getPet().getName(),
                            a.getServiceOrder().getOwner().getFullName(),
                            rt.getCheckedInAt(),
                            a.getSymptomText()
                    );
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ServiceCatalogOptionResponse> listServicesByCategory(ServiceCategory category) {
        return serviceCatalogRepository.findByCategoryCodeAndIsActiveTrueOrderByNameAsc(category).stream()
                .map(assembler::toServiceCatalogOptionResponse)
                .toList();
    }

    private String normalizePhone(String phone) {
        if (phone == null) {
            return "";
        }
        return phone.replaceAll("[\\s.\\-]", "");
    }

    private boolean matchesPhone(String ownerPhone, String needle) {
        return normalizePhone(ownerPhone).contains(needle);
    }

    private boolean containsIgnoreCase(String value, String needle) {
        return value != null && value.toLowerCase().contains(needle);
    }
}
