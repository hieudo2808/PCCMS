package com.astral.express.pccms.boarding.service;

import com.astral.express.pccms.boarding.dto.response.BoardingStayResponse;
import com.astral.express.pccms.boarding.dto.response.CareLogResponse;
import com.astral.express.pccms.boarding.entity.CareLog;
import com.astral.express.pccms.boarding.repository.CareLogRepository;
import com.astral.express.pccms.pet.entity.Pets;
import com.astral.express.pccms.pet.repository.PetRepository;
import com.astral.express.pccms.user.entity.Users;
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

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvFileSource(resources = "/testcases/boarding-tracking-testcases.csv", numLinesToSkip = 1)
    void should_ProcessBoardingTracking(String ruleId, String caseId, String action, boolean hasData, int expectedSize) {
        UUID ownerId = UUID.randomUUID();

        if ("LIST_ACTIVE_STAYS".equals(action)) {
            if (hasData) {
                UUID petId = UUID.randomUUID();
                Object[] row = new Object[]{petId, "Milu", "Chó", "Poodle"};
                given(careLogRepository.findActiveStaysByOwner(ownerId)).willReturn(List.<Object[]>of(row));
            } else {
                given(careLogRepository.findActiveStaysByOwner(ownerId)).willReturn(List.of());
            }

            List<BoardingStayResponse> stays = boardingTrackingService.listActiveStays(ownerId);

            assertThat(stays).hasSize(expectedSize);
            if (expectedSize > 0) {
                assertThat(stays.get(0).petName()).isEqualTo("Milu");
                assertThat(stays.get(0).speciesName()).isEqualTo("Chó");
                assertThat(stays.get(0).breedName()).isEqualTo("Poodle");
            }
        } else if ("LIST_CARE_LOGS".equals(action)) {
            if (hasData) {
                UUID petId = UUID.randomUUID();
                UUID logId = UUID.randomUUID();

                CareLog log = new CareLog();
                log.setId(logId);
                
                Pets pet = new Pets();
                pet.setId(petId);
                pet.setName("Milu");
                log.setPet(pet);
                
                com.astral.express.pccms.boarding.entity.BoardingSession session = new com.astral.express.pccms.boarding.entity.BoardingSession();
                session.setId(UUID.randomUUID());
                log.setSession(session);
                
                Users staff = new Users();
                staff.setId(UUID.randomUUID());
                staff.setFullName("StaffName");
                log.setStaff(staff);

                log.setLogDate(LocalDate.of(2026, 6, 5));
                log.setPeriodCode(com.astral.express.pccms.boarding.entity.CarePeriod.MORNING);
                log.setFeedingStatus("Ăn tốt");
                log.setHygieneStatus("Bình thường");
                log.setHealthNote(null);
                log.setStaffNote("Ghi chú nhân viên");

                given(careLogRepository.findActiveStayLogsByOwner(ownerId, null)).willReturn(List.of(log));
            } else {
                given(careLogRepository.findActiveStayLogsByOwner(ownerId, null)).willReturn(List.of());
            }

            List<CareLogResponse> responses = boardingTrackingService.listCareLogs(ownerId, null);

            assertThat(responses).hasSize(expectedSize);
            if (expectedSize > 0) {
                assertThat(responses.get(0).feedingStatus()).isEqualTo("Ăn tốt");
            }
        }
    }
}
