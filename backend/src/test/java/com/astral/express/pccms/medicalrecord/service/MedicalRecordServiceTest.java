package com.astral.express.pccms.medicalrecord.service;

import com.astral.express.pccms.medicalrecord.dto.response.MedicalRecordResponse;
import com.astral.express.pccms.medicalrecord.repository.MedicalRecordRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MedicalRecordServiceTest {

    @Mock
    private MedicalRecordRepository medicalRecordRepository;

    @InjectMocks
    private MedicalRecordService medicalRecordService;

    @Test
    void should_FetchResponsesByVetId_when_VetIdIsProvided() {
        // GIVEN
        UUID vetId = UUID.randomUUID();
        MedicalRecordResponse mockResponse1 = new MedicalRecordResponse(UUID.randomUUID(), null, null, null, "Rex", vetId, "Dr. Smith", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
        MedicalRecordResponse mockResponse2 = new MedicalRecordResponse(UUID.randomUUID(), null, null, null, "Bella", vetId, "Dr. Smith", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);

        when(medicalRecordRepository.findResponsesByVetId(vetId)).thenReturn(List.of(mockResponse1, mockResponse2));

        // WHEN
        List<MedicalRecordResponse> results = medicalRecordService.getMedicalRecords(vetId);

        // THEN
        assertThat(results).hasSize(2);
        assertThat(results.get(0).petName()).isEqualTo("Rex");
        assertThat(results.get(1).petName()).isEqualTo("Bella");
        verify(medicalRecordRepository).findResponsesByVetId(vetId);
    }

    @Test
    void should_FetchAllResponses_when_VetIdIsNull() {
        // GIVEN
        MedicalRecordResponse mockResponse1 = new MedicalRecordResponse(UUID.randomUUID(), null, null, null, "Rex", null, "Dr. Smith", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
        
        when(medicalRecordRepository.findAllResponses()).thenReturn(List.of(mockResponse1));

        // WHEN
        List<MedicalRecordResponse> results = medicalRecordService.getMedicalRecords(null);

        // THEN
        assertThat(results).hasSize(1);
        verify(medicalRecordRepository).findAllResponses();
    }
}


