package com.astral.express.pccms.boarding.service;

import com.astral.express.pccms.boarding.dto.response.BoardingStayResponse;
import com.astral.express.pccms.boarding.dto.response.CareLogResponse;
import com.astral.express.pccms.boarding.entity.CareLog;
import com.astral.express.pccms.boarding.repository.CareLogRepository;
import com.astral.express.pccms.pet.entity.Pets;
import com.astral.express.pccms.pet.repository.PetRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BoardingTrackingServiceTest {

    @Mock
    private CareLogRepository careLogRepository;

    @Mock
    private PetRepository petRepository;

    @InjectMocks
    private BoardingTrackingServiceImpl boardingTrackingService;

    @Test
    void listActiveStays_mapsNativeQueryRows() {
        UUID ownerId = UUID.randomUUID();
        UUID petId = UUID.randomUUID();
        Object[] row = new Object[]{petId, "Milu", "Chó", "Poodle"};
        given(careLogRepository.findActiveStaysByOwner(ownerId)).willReturn(List.<Object[]>of(row));

        List<BoardingStayResponse> stays = boardingTrackingService.listActiveStays(ownerId);

        assertThat(stays).hasSize(1);
        assertThat(stays.get(0).petName()).isEqualTo("Milu");
        assertThat(stays.get(0).speciesName()).isEqualTo("Chó");
        assertThat(stays.get(0).breedName()).isEqualTo("Poodle");
    }

    @Test
    void listCareLogs_returnsMappedResponses() {
        UUID ownerId = UUID.randomUUID();
        UUID petId = UUID.randomUUID();
        UUID logId = UUID.randomUUID();

        CareLog log = mock(CareLog.class);
        when(log.getId()).thenReturn(logId);
        when(log.getPetId()).thenReturn(petId);
        when(log.getLogDate()).thenReturn(LocalDate.of(2026, 6, 5));
        when(log.getPeriodCode()).thenReturn("MORNING");
        when(log.getFeedingStatus()).thenReturn("Ăn tốt");
        when(log.getHygieneStatus()).thenReturn("Bình thường");
        when(log.getHealthNote()).thenReturn(null);
        when(log.getStaffNote()).thenReturn("Ghi chú nhân viên");

        Pets pet = new Pets();
        pet.setId(petId);
        pet.setName("Milu");

        given(careLogRepository.findActiveStayLogsByOwner(ownerId, null)).willReturn(List.of(log));
        given(petRepository.findAllById(List.of(petId))).willReturn(List.of(pet));

        List<CareLogResponse> responses = boardingTrackingService.listCareLogs(ownerId, null);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).petName()).isEqualTo("Milu");
        assertThat(responses.get(0).periodLabel()).isEqualTo("Sáng");
        assertThat(responses.get(0).feedingStatus()).isEqualTo("Ăn tốt");
    }

    @Test
    void listCareLogs_returnsEmptyWhenNoLogs() {
        UUID ownerId = UUID.randomUUID();
        given(careLogRepository.findActiveStayLogsByOwner(ownerId, null)).willReturn(List.of());

        assertThat(boardingTrackingService.listCareLogs(ownerId, null)).isEmpty();
    }
}
