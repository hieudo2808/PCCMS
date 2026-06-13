package com.astral.express.pccms.appointment.service;

import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.schedule.repository.WorkScheduleRepository;
import com.astral.express.pccms.user.entity.Roles;
import com.astral.express.pccms.user.entity.Users;
import com.astral.express.pccms.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class VetAvailabilityCheckerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private WorkScheduleRepository workScheduleRepository;

    @Mock
    private AppointmentOverlapChecker overlapChecker;

    @InjectMocks
    private VetAvailabilityChecker checker;

    @Test
    void isVetOnDuty_shouldReturnTrue_whenInSchedule() {
        UUID vetId = UUID.randomUUID();
        LocalDate date = LocalDate.now();
        LocalTime time = LocalTime.now();

        given(workScheduleRepository.findAvailableVetIds(date, time)).willReturn(List.of(vetId));

        boolean result = checker.isVetOnDuty(date, time, vetId);

        assertThat(result).isTrue();
    }

    @Test
    void isVetOnDuty_shouldReturnTrue_whenNoScheduleButActive() {
        UUID vetId = UUID.randomUUID();
        LocalDate date = LocalDate.now();
        LocalTime time = LocalTime.now();

        Users vet = new Users();
        vet.setId(vetId);

        given(workScheduleRepository.findAvailableVetIds(date, time)).willReturn(List.of());
        given(userRepository.findActiveByRoleCode("VETERINARIAN")).willReturn(List.of(vet));

        boolean result = checker.isVetOnDuty(date, time, vetId);

        assertThat(result).isTrue();
    }

    @Test
    void isVetOnDuty_shouldReturnFalse_whenNotOnDuty() {
        UUID vetId = UUID.randomUUID();
        LocalDate date = LocalDate.now();
        LocalTime time = LocalTime.now();

        given(workScheduleRepository.findAvailableVetIds(date, time)).willReturn(List.of(UUID.randomUUID()));

        boolean result = checker.isVetOnDuty(date, time, vetId);

        assertThat(result).isFalse();
    }

    @Test
    void resolveVetCandidates_shouldReturnScheduleIds() {
        UUID vetId = UUID.randomUUID();
        given(workScheduleRepository.findAvailableVetIds(any(), any())).willReturn(List.of(vetId));

        List<UUID> result = checker.resolveVetCandidates(LocalDate.now(), LocalTime.now());

        assertThat(result).containsExactly(vetId);
    }

    @Test
    void resolveVetCandidates_shouldReturnActiveVets_whenNoSchedule() {
        UUID vetId = UUID.randomUUID();
        Users vet = new Users();
        vet.setId(vetId);

        given(workScheduleRepository.findAvailableVetIds(any(), any())).willReturn(List.of());
        given(userRepository.findActiveByRoleCode("VETERINARIAN")).willReturn(List.of(vet));

        List<UUID> result = checker.resolveVetCandidates(LocalDate.now(), LocalTime.now());

        assertThat(result).containsExactly(vetId);
    }

    @Test
    void requireVetAvailable_shouldThrowException_whenRequestedVetNotFound() {
        UUID vetId = UUID.randomUUID();
        given(userRepository.findById(vetId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> checker.requireVetAvailable(LocalDate.now(), LocalTime.now(), vetId, OffsetDateTime.now(), OffsetDateTime.now()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_ACC_002_USER_NOT_FOUND);
    }

    @Test
    void requireVetAvailable_shouldThrowException_whenRequestedUserNotVet() {
        UUID vetId = UUID.randomUUID();
        Users user = new Users();
        Roles role = new Roles();
        role.setCode("ADMIN");
        user.setRole(role);
        given(userRepository.findById(vetId)).willReturn(Optional.of(user));

        assertThatThrownBy(() -> checker.requireVetAvailable(LocalDate.now(), LocalTime.now(), vetId, OffsetDateTime.now(), OffsetDateTime.now()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_APT_005_NO_VET_AVAILABLE);
    }

    @Test
    void requireVetAvailable_shouldThrowException_whenRequestedVetNotOnDuty() {
        UUID vetId = UUID.randomUUID();
        Users user = new Users();
        Roles role = new Roles();
        role.setCode("VETERINARIAN");
        user.setRole(role);
        given(userRepository.findById(vetId)).willReturn(Optional.of(user));
        given(workScheduleRepository.findAvailableVetIds(any(), any())).willReturn(List.of(UUID.randomUUID()));

        assertThatThrownBy(() -> checker.requireVetAvailable(LocalDate.now(), LocalTime.now(), vetId, OffsetDateTime.now(), OffsetDateTime.now()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_APT_009_SLOT_FULL);
    }

    @Test
    void requireVetAvailable_shouldReturnVet_whenRequestedVetAvailable() {
        UUID vetId = UUID.randomUUID();
        Users user = new Users();
        Roles role = new Roles();
        role.setCode("VETERINARIAN");
        user.setRole(role);
        given(userRepository.findById(vetId)).willReturn(Optional.of(user));
        given(workScheduleRepository.findAvailableVetIds(any(), any())).willReturn(List.of(vetId));
        given(overlapChecker.hasVetOverlap(eq(vetId), any(), any())).willReturn(false);

        Users result = checker.requireVetAvailable(LocalDate.now(), LocalTime.now(), vetId, OffsetDateTime.now(), OffsetDateTime.now());

        assertThat(result).isEqualTo(user);
    }

    @Test
    void requireVetAvailable_shouldThrowException_whenNoVetCandidates() {
        given(workScheduleRepository.findAvailableVetIds(any(), any())).willReturn(List.of());
        given(userRepository.findActiveByRoleCode("VETERINARIAN")).willReturn(List.of());

        assertThatThrownBy(() -> checker.requireVetAvailable(LocalDate.now(), LocalTime.now(), null, OffsetDateTime.now(), OffsetDateTime.now()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_APT_005_NO_VET_AVAILABLE);
    }

    @Test
    void requireVetAvailable_shouldReturnAnyAvailableVet() {
        UUID vetId = UUID.randomUUID();
        Users user = new Users();
        user.setId(vetId);

        given(workScheduleRepository.findAvailableVetIds(any(), any())).willReturn(List.of(vetId));
        given(userRepository.findById(vetId)).willReturn(Optional.of(user));
        given(overlapChecker.hasVetOverlap(eq(vetId), any(), any())).willReturn(false);

        Users result = checker.requireVetAvailable(LocalDate.now(), LocalTime.now(), null, OffsetDateTime.now(), OffsetDateTime.now());

        assertThat(result).isEqualTo(user);
    }

    @Test
    void isVetFree_shouldReturnTrue() {
        given(overlapChecker.hasVetOverlap(any(), any(), any())).willReturn(false);
        boolean result = checker.isVetFree(UUID.randomUUID(), OffsetDateTime.now(), OffsetDateTime.now());
        assertThat(result).isTrue();
    }

    @Test
    void findVetIdsOnDutyForDate_shouldReturnIds() {
        UUID vetId = UUID.randomUUID();
        given(workScheduleRepository.findVetIdsOnDutyForDate(any())).willReturn(List.of(vetId));
        List<UUID> result = checker.findVetIdsOnDutyForDate(LocalDate.now());
        assertThat(result).containsExactly(vetId);
    }
}
