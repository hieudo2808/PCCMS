package com.astral.express.pccms.medicalrecord.service;

import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.filemedia.entity.FileAsset;
import com.astral.express.pccms.filemedia.repository.FileAssetRepository;
import com.astral.express.pccms.identity.security.SecurityContextService;
import com.astral.express.pccms.medicalrecord.dto.request.CreateLabResultRequest;
import com.astral.express.pccms.medicalrecord.dto.response.LabResultResponse;
import com.astral.express.pccms.medicalrecord.entity.LabResult;
import com.astral.express.pccms.medicalrecord.entity.MedicalRecord;
import com.astral.express.pccms.medicalrecord.repository.LabResultRepository;
import com.astral.express.pccms.medicalrecord.repository.MedicalRecordRepository;
import com.astral.express.pccms.pet.entity.Pets;
import com.astral.express.pccms.pet.repository.PetRepository;
import com.astral.express.pccms.user.entity.Users;
import com.astral.express.pccms.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LabResultServiceTest {

    @Mock
    private LabResultRepository labResultRepository;
    @Mock
    private MedicalRecordRepository medicalRecordRepository;
    @Mock
    private FileAssetRepository fileAssetRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PetRepository petRepository;
    @Mock
    private SecurityContextService securityContextService;

    @InjectMocks
    private LabResultService labResultService;

    @Test
    void should_ListLabResults_when_UserIsAdmin() {
        // GIVEN
        UUID recordId = UUID.randomUUID();
        MedicalRecord record = MedicalRecord.builder().id(recordId).build();
        given(medicalRecordRepository.findById(recordId)).willReturn(Optional.of(record));

        given(securityContextService.hasAnyRole("ADMIN", "VETERINARIAN")).willReturn(true);

        LabResult labResult = LabResult.builder().id(UUID.randomUUID()).medicalRecord(record).testName("Blood Test").build();
        given(labResultRepository.findByMedicalRecordIdOrderByCreatedAtDesc(recordId))
                .willReturn(List.of(labResult));

        // WHEN
        List<LabResultResponse> result = labResultService.listLabResults(recordId);

        // THEN
        assertThat(result).hasSize(1);
        assertThat(result.get(0).testName()).isEqualTo("Blood Test");
    }

    @Test
    void should_ListLabResults_when_UserIsOwner() {
        // GIVEN
        UUID recordId = UUID.randomUUID();
        UUID petId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        
        MedicalRecord record = MedicalRecord.builder().id(recordId).petId(petId).build();
        given(medicalRecordRepository.findById(recordId)).willReturn(Optional.of(record));

        given(securityContextService.hasAnyRole("ADMIN", "VETERINARIAN")).willReturn(false);
        given(securityContextService.getCurrentUserId()).willReturn(ownerId);

        Users owner = Users.builder().id(ownerId).build();
        Pets pet = Pets.builder().id(petId).owner(owner).build();
        given(petRepository.findById(petId)).willReturn(Optional.of(pet));

        LabResult labResult = LabResult.builder().id(UUID.randomUUID()).medicalRecord(record).testName("Blood Test").build();
        given(labResultRepository.findByMedicalRecordIdOrderByCreatedAtDesc(recordId))
                .willReturn(List.of(labResult));

        // WHEN
        List<LabResultResponse> result = labResultService.listLabResults(recordId);

        // THEN
        assertThat(result).hasSize(1);
    }

    @Test
    void should_ThrowException_when_ListLabResults_AndUserIsNotOwner() {
        // GIVEN
        UUID recordId = UUID.randomUUID();
        UUID petId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID otherId = UUID.randomUUID();
        
        MedicalRecord record = MedicalRecord.builder().id(recordId).petId(petId).build();
        given(medicalRecordRepository.findById(recordId)).willReturn(Optional.of(record));

        given(securityContextService.hasAnyRole("ADMIN", "VETERINARIAN")).willReturn(false);
        given(securityContextService.getCurrentUserId()).willReturn(otherId);

        Users owner = Users.builder().id(ownerId).build();
        Pets pet = Pets.builder().id(petId).owner(owner).build();
        given(petRepository.findById(petId)).willReturn(Optional.of(pet));

        // WHEN & THEN
        assertThatThrownBy(() -> labResultService.listLabResults(recordId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_403_FORBIDDEN);
    }

    @Test
    void should_CreateLabResult_Success() {
        // GIVEN
        UUID recordId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();
        UUID fileId = UUID.randomUUID();

        MedicalRecord record = MedicalRecord.builder().id(recordId).build();
        given(medicalRecordRepository.findById(recordId)).willReturn(Optional.of(record));

        given(securityContextService.getCurrentUserId()).willReturn(creatorId);
        Users creator = Users.builder().id(creatorId).build();
        given(userRepository.findById(creatorId)).willReturn(Optional.of(creator));

        FileAsset file = FileAsset.builder().id(fileId).build();
        given(fileAssetRepository.findById(fileId)).willReturn(Optional.of(file));

        CreateLabResultRequest request = new CreateLabResultRequest("X-Ray ", "Clear", fileId);

        LabResult savedResult = LabResult.builder()
                .id(UUID.randomUUID())
                .medicalRecord(record)
                .testName("X-Ray")
                .resultText("Clear")
                .file(file)
                .createdBy(creator)
                .build();
        given(labResultRepository.save(any(LabResult.class))).willReturn(savedResult);

        // WHEN
        LabResultResponse response = labResultService.createLabResult(recordId, request);

        // THEN
        assertThat(response.testName()).isEqualTo("X-Ray");
        assertThat(response.resultText()).isEqualTo("Clear");
        assertThat(response.fileId()).isEqualTo(fileId);
    }

    @Test
    void should_ThrowException_when_CreateLabResult_AndUserNotFound() {
        UUID recordId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();

        MedicalRecord record = MedicalRecord.builder().id(recordId).build();
        given(medicalRecordRepository.findById(recordId)).willReturn(Optional.of(record));

        given(securityContextService.getCurrentUserId()).willReturn(creatorId);
        given(userRepository.findById(creatorId)).willReturn(Optional.empty());

        CreateLabResultRequest request = new CreateLabResultRequest("X-Ray", "Clear", null);

        assertThatThrownBy(() -> labResultService.createLabResult(recordId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_ACC_002_USER_NOT_FOUND);
    }

    @Test
    void should_CreateLabResult_WithNullFile() {
        UUID recordId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();

        MedicalRecord record = MedicalRecord.builder().id(recordId).build();
        given(medicalRecordRepository.findById(recordId)).willReturn(Optional.of(record));

        given(securityContextService.getCurrentUserId()).willReturn(creatorId);
        Users creator = Users.builder().id(creatorId).build();
        given(userRepository.findById(creatorId)).willReturn(Optional.of(creator));

        CreateLabResultRequest request = new CreateLabResultRequest("X-Ray", "Clear", null);

        LabResult savedResult = LabResult.builder()
                .id(UUID.randomUUID())
                .medicalRecord(record)
                .testName("X-Ray")
                .resultText("Clear")
                .file(null)
                .createdBy(creator)
                .build();
        given(labResultRepository.save(any(LabResult.class))).willReturn(savedResult);

        LabResultResponse response = labResultService.createLabResult(recordId, request);

        assertThat(response.fileId()).isNull();
    }

    @Test
    void should_ThrowException_when_CreateLabResult_AndFileNotFound() {
        UUID recordId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();
        UUID fileId = UUID.randomUUID();

        MedicalRecord record = MedicalRecord.builder().id(recordId).build();
        given(medicalRecordRepository.findById(recordId)).willReturn(Optional.of(record));

        given(securityContextService.getCurrentUserId()).willReturn(creatorId);
        Users creator = Users.builder().id(creatorId).build();
        given(userRepository.findById(creatorId)).willReturn(Optional.of(creator));

        given(fileAssetRepository.findById(fileId)).willReturn(Optional.empty());

        CreateLabResultRequest request = new CreateLabResultRequest("X-Ray", "Clear", fileId);

        assertThatThrownBy(() -> labResultService.createLabResult(recordId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_404_NOT_FOUND);
    }

    @Test
    void should_ThrowException_when_FindMedicalRecord_NotFound() {
        UUID recordId = UUID.randomUUID();
        given(medicalRecordRepository.findById(recordId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> labResultService.listLabResults(recordId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_404_NOT_FOUND);
    }

    @Test
    void should_ThrowException_when_AssertCanRead_AndPetNotFound() {
        UUID recordId = UUID.randomUUID();
        UUID petId = UUID.randomUUID();
        
        MedicalRecord record = MedicalRecord.builder().id(recordId).petId(petId).build();
        given(medicalRecordRepository.findById(recordId)).willReturn(Optional.of(record));

        given(securityContextService.hasAnyRole("ADMIN", "VETERINARIAN")).willReturn(false);
        given(securityContextService.getCurrentUserId()).willReturn(UUID.randomUUID());

        given(petRepository.findById(petId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> labResultService.listLabResults(recordId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_PET_001_NOT_FOUND);
    }

    @Test
    void should_ThrowException_when_RequireCurrentUserId_IsNull() {
        UUID recordId = UUID.randomUUID();
        
        MedicalRecord record = MedicalRecord.builder().id(recordId).petId(UUID.randomUUID()).build();
        given(medicalRecordRepository.findById(recordId)).willReturn(Optional.of(record));

        given(securityContextService.hasAnyRole("ADMIN", "VETERINARIAN")).willReturn(false);
        given(securityContextService.getCurrentUserId()).willReturn(null);

        assertThatThrownBy(() -> labResultService.listLabResults(recordId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_401_UNAUTHORIZED);
    }
}
