package com.astral.express.pccms.grooming.service;

import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.grooming.dto.request.GroomingStationRequest;
import com.astral.express.pccms.grooming.entity.GroomingStation;
import com.astral.express.pccms.grooming.mapper.GroomingMapper;
import com.astral.express.pccms.grooming.repository.GroomingStationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GroomingStationAdminServiceTest {

    @Mock
    private GroomingStationRepository groomingStationRepository;

    private GroomingStationAdminService service;

    @BeforeEach
    void setUp() {
        service = new GroomingStationAdminService(groomingStationRepository, new GroomingMapper());
    }

    @Test
    void createStation_rejectsDuplicateCode() {
        GroomingStationRequest request = new GroomingStationRequest("SPA-01", "Ban spa 1", true);
        given(groomingStationRepository.existsByStationCode("SPA-01")).willReturn(true);

        assertThatThrownBy(() -> service.createStation(request))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ERR_GROOMING_008_STATION_CODE_EXISTS));
    }

    @Test
    void updateStation_updatesAllAdminEditableFields() {
        UUID id = UUID.randomUUID();
        GroomingStation station = GroomingStation.builder()
                .id(id)
                .stationCode("SPA-01")
                .name("Ban spa 1")
                .isActive(true)
                .build();
        given(groomingStationRepository.findById(id)).willReturn(Optional.of(station));
        given(groomingStationRepository.existsByStationCodeAndIdNot("SPA-02", id)).willReturn(false);
        given(groomingStationRepository.save(any(GroomingStation.class))).willAnswer(invocation -> invocation.getArgument(0));

        var response = service.updateStation(id, new GroomingStationRequest("SPA-02", "Phong cham soc", false));

        assertThat(response.stationCode()).isEqualTo("SPA-02");
        assertThat(response.name()).isEqualTo("Phong cham soc");
        assertThat(response.isActive()).isFalse();
    }

    @Test
    void deactivateStation_softDisablesStation() {
        UUID id = UUID.randomUUID();
        GroomingStation station = GroomingStation.builder()
                .id(id)
                .stationCode("SPA-01")
                .name("Ban spa 1")
                .isActive(true)
                .build();
        given(groomingStationRepository.findById(id)).willReturn(Optional.of(station));

        service.deactivateStation(id);

        ArgumentCaptor<GroomingStation> captor = ArgumentCaptor.forClass(GroomingStation.class);
        verify(groomingStationRepository).save(captor.capture());
        assertThat(captor.getValue().getIsActive()).isFalse();
    }
}
