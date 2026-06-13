package com.astral.express.pccms.room.service;

import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.room.dto.request.RoomRequest;
import com.astral.express.pccms.room.dto.request.RoomStatusUpdateRequest;
import com.astral.express.pccms.room.dto.request.RoomTypeRequest;
import com.astral.express.pccms.room.dto.response.RoomResponse;
import com.astral.express.pccms.room.dto.response.RoomTypeResponse;
import com.astral.express.pccms.room.entity.Room;
import com.astral.express.pccms.room.entity.RoomStatus;
import com.astral.express.pccms.room.entity.RoomType;
import com.astral.express.pccms.room.mapper.RoomMapper;
import com.astral.express.pccms.room.repository.RoomRepository;
import com.astral.express.pccms.room.repository.RoomTypeRepository;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.mockito.InjectMocks;
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
class RoomAdminServiceTest {

    @Mock
    private RoomTypeRepository roomTypeRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private RoomMapper roomMapper;

    @InjectMocks
    private RoomAdminService roomAdminService;

    @ParameterizedTest(name = "[{1}] {3}")
    @CsvFileSource(resources = "/testcases/room-admin.csv", numLinesToSkip = 1)
    void should_followRoomAdminCsvRules(
            String ruleId,
            String caseId,
            String useCase,
            String scenario,
            String precondition,
            String input,
            String expectedResult,
            String expectedErrorCode,
            String expectedMessage,
            String note) {

        RoomAdminCsvInput csv = parseInput(input);

        switch (scenario) {
            case "Create room type success" -> assertCreateRoomTypeSuccess(csv);
            case "Create room type duplicate code" -> assertCreateRoomTypeFailure(csv, ErrorCode.valueOf(expectedErrorCode));
            case "Create room type invalid capacity", "Create room type invalid price" -> assertCreateRoomTypeValidation(csv, ErrorCode.valueOf(expectedErrorCode));
            case "Update room type success" -> assertUpdateRoomTypeSuccess(csv);
            case "Update room type duplicate code" -> assertUpdateRoomTypeFailure(csv, ErrorCode.valueOf(expectedErrorCode));
            case "Update room type active success" -> assertUpdateRoomTypeActiveSuccess(csv);
            case "Deactivate room type success" -> assertDeactivateRoomTypeSuccess(csv);
            case "Deactivate room type in use" -> assertDeactivateRoomTypeFailure(csv, ErrorCode.valueOf(expectedErrorCode));
            case "Create room success" -> assertCreateRoomSuccess(csv);
            case "Create room invalid capacity" -> assertCreateRoomValidation(csv, ErrorCode.valueOf(expectedErrorCode));
            case "Create room room type not found" -> assertCreateRoomFailure(csv, ErrorCode.valueOf(expectedErrorCode));
            case "Update room success" -> assertUpdateRoomSuccess(csv);
            case "Update room not found" -> assertUpdateRoomFailure(csv, ErrorCode.valueOf(expectedErrorCode));
            case "Update room status success" -> assertUpdateRoomStatusSuccess(csv);
            default -> throw new IllegalArgumentException("Unhandled CSV scenario: " + scenario);
        }
    }

    private void assertCreateRoomTypeSuccess(RoomAdminCsvInput csv) {
        RoomTypeRequest request = roomTypeRequest(csv);
        given(roomTypeRepository.existsByCodeAndIsActiveTrue(csv.code())).willReturn(false);
        given(roomMapper.toRoomType(request)).willReturn(new RoomType());
        RoomType saved = new RoomType();
        saved.setCode(csv.code());
        given(roomTypeRepository.save(any(RoomType.class))).willReturn(saved);
        given(roomMapper.toRoomTypeResponse(saved)).willReturn(new RoomTypeResponse(UUID.randomUUID(), csv.code(), csv.name(), csv.defaultCapacity(), csv.baseDailyPriceVnd(), null, true));

        RoomTypeResponse response = roomAdminService.createRoomType(request);

        assertThat(response.code()).isEqualTo(csv.code());
        verify(roomTypeRepository).save(any(RoomType.class));
    }

    private void assertCreateRoomTypeFailure(RoomAdminCsvInput csv, ErrorCode errorCode) {
        RoomTypeRequest request = roomTypeRequest(csv);
        given(roomTypeRepository.existsByCodeAndIsActiveTrue(csv.code())).willReturn(true);

        assertThatThrownBy(() -> roomAdminService.createRoomType(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", errorCode);
    }

    private void assertCreateRoomTypeValidation(RoomAdminCsvInput csv, ErrorCode errorCode) {
        RoomTypeRequest request = roomTypeRequest(csv);
        assertThatThrownBy(() -> roomAdminService.createRoomType(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", errorCode);
    }

    private void assertUpdateRoomTypeSuccess(RoomAdminCsvInput csv) {
        UUID id = csv.id();
        RoomTypeRequest request = roomTypeRequest(csv);
        RoomType roomType = roomType(id);
        given(roomTypeRepository.findById(id)).willReturn(Optional.of(roomType));
        given(roomTypeRepository.existsByCodeAndIdNot(csv.code(), id)).willReturn(false);
        given(roomTypeRepository.save(any(RoomType.class))).willReturn(roomType);
        given(roomMapper.toRoomTypeResponse(roomType)).willReturn(new RoomTypeResponse(id, csv.code(), csv.name(), csv.defaultCapacity(), csv.baseDailyPriceVnd(), null, true));

        RoomTypeResponse response = roomAdminService.updateRoomType(id, request);

        assertThat(response.id()).isEqualTo(id);
        verify(roomMapper).updateRoomType(request, roomType);
    }

    private void assertUpdateRoomTypeFailure(RoomAdminCsvInput csv, ErrorCode errorCode) {
        UUID id = csv.id();
        RoomTypeRequest request = roomTypeRequest(csv);
        given(roomTypeRepository.findById(id)).willReturn(Optional.of(roomType(id)));
        given(roomTypeRepository.existsByCodeAndIdNot(csv.code(), id)).willReturn(true);

        assertThatThrownBy(() -> roomAdminService.updateRoomType(id, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", errorCode);
    }

    private void assertUpdateRoomTypeActiveSuccess(RoomAdminCsvInput csv) {
        UUID id = csv.id();
        RoomType roomType = roomType(id);
        given(roomTypeRepository.findById(id)).willReturn(Optional.of(roomType));
        given(roomTypeRepository.save(any(RoomType.class))).willReturn(roomType);
        given(roomMapper.toRoomTypeResponse(roomType)).willReturn(new RoomTypeResponse(id, "C", "N", 2, 1L, null, false));

        roomAdminService.updateRoomTypeActive(id, csv.isActive());

        verify(roomTypeRepository).save(any(RoomType.class));
    }

    private void assertDeactivateRoomTypeSuccess(RoomAdminCsvInput csv) {
        UUID id = csv.id();
        RoomType roomType = roomType(id);
        given(roomTypeRepository.findById(id)).willReturn(Optional.of(roomType));
        given(roomRepository.existsByRoomTypeId(id)).willReturn(false);

        roomAdminService.deactivateRoomType(id);

        verify(roomTypeRepository).save(roomType);
        assertThat(roomType.getIsActive()).isFalse();
    }

    private void assertDeactivateRoomTypeFailure(RoomAdminCsvInput csv, ErrorCode errorCode) {
        UUID id = csv.id();
        RoomType roomType = roomType(id);
        given(roomTypeRepository.findById(id)).willReturn(Optional.of(roomType));
        given(roomRepository.existsByRoomTypeId(id)).willReturn(true);

        assertThatThrownBy(() -> roomAdminService.deactivateRoomType(id))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", errorCode);
    }

    private void assertCreateRoomSuccess(RoomAdminCsvInput csv) {
        RoomRequest request = roomRequest(csv);
        RoomType roomType = roomType(csv.roomTypeId());
        given(roomTypeRepository.findByIdAndIsActiveTrue(csv.roomTypeId())).willReturn(Optional.of(roomType));
        Room room = new Room();
        given(roomMapper.toRoom(request)).willReturn(room);
        given(roomRepository.save(any(Room.class))).willReturn(room);
        given(roomMapper.toRoomResponse(room)).willReturn(new RoomResponse(UUID.randomUUID(), csv.roomCode(), csv.name(), roomType.getId(), roomType.getName(), csv.floor(), csv.capacity(), csv.statusCode(), null));

        RoomResponse response = roomAdminService.createRoom(request);

        assertThat(response.roomCode()).isEqualTo(csv.roomCode());
        verify(roomRepository).save(any(Room.class));
    }

    private void assertCreateRoomValidation(RoomAdminCsvInput csv, ErrorCode errorCode) {
        RoomRequest request = roomRequest(csv);
        assertThatThrownBy(() -> roomAdminService.createRoom(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", errorCode);
    }

    private void assertCreateRoomFailure(RoomAdminCsvInput csv, ErrorCode errorCode) {
        RoomRequest request = roomRequest(csv);
        given(roomTypeRepository.findByIdAndIsActiveTrue(csv.roomTypeId())).willReturn(Optional.empty());

        assertThatThrownBy(() -> roomAdminService.createRoom(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", errorCode);
    }

    private void assertUpdateRoomSuccess(RoomAdminCsvInput csv) {
        UUID id = csv.id();
        RoomRequest request = roomRequest(csv);
        Room room = new Room();
        RoomType roomType = roomType(csv.roomTypeId());
        given(roomRepository.findWithRoomTypeById(id)).willReturn(Optional.of(room));
        given(roomTypeRepository.findByIdAndIsActiveTrue(csv.roomTypeId())).willReturn(Optional.of(roomType));
        given(roomRepository.save(any(Room.class))).willReturn(room);
        given(roomMapper.toRoomResponse(room)).willReturn(new RoomResponse(id, csv.roomCode(), csv.name(), roomType.getId(), roomType.getName(), csv.floor(), csv.capacity(), csv.statusCode(), null));

        RoomResponse response = roomAdminService.updateRoom(id, request);

        assertThat(response.id()).isEqualTo(id);
        verify(roomMapper).updateRoom(request, room);
    }

    private void assertUpdateRoomFailure(RoomAdminCsvInput csv, ErrorCode errorCode) {
        UUID id = csv.id();
        RoomRequest request = roomRequest(csv);
        given(roomRepository.findWithRoomTypeById(id)).willReturn(Optional.empty());

        assertThatThrownBy(() -> roomAdminService.updateRoom(id, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", errorCode);
    }

    private void assertUpdateRoomStatusSuccess(RoomAdminCsvInput csv) {
        UUID id = csv.id();
        Room room = new Room();
        given(roomRepository.findWithRoomTypeById(id)).willReturn(Optional.of(room));
        given(roomRepository.save(room)).willReturn(room);
        given(roomMapper.toRoomResponse(room)).willReturn(new RoomResponse(id, null, null, null, null, 1, 1, csv.statusCode(), null));

        RoomStatusUpdateRequest request = new RoomStatusUpdateRequest(csv.statusCode());
        RoomResponse response = roomAdminService.updateRoomStatus(id, request);

        assertThat(response.statusCode()).isEqualTo(csv.statusCode());
    }

    private RoomType roomType(UUID id) {
        RoomType type = new RoomType();
        type.setId(id);
        type.setCode("STD");
        type.setName("Standard");
        type.setDefaultCapacity(2);
        type.setBaseDailyPriceVnd(100000L);
        type.setIsActive(true);
        return type;
    }

    private RoomTypeRequest roomTypeRequest(RoomAdminCsvInput input) {
        return new RoomTypeRequest(
                input.code(),
                input.name(),
                input.defaultCapacity(),
                input.baseDailyPriceVnd(),
                null,
                input.isActive()
        );
    }

    private RoomRequest roomRequest(RoomAdminCsvInput input) {
        return new RoomRequest(
                input.roomCode(),
                input.name(),
                input.roomTypeId(),
                input.floor(),
                input.capacity(),
                input.statusCode(),
                null
        );
    }

    private RoomAdminCsvInput parseInput(String input) {
        return new RoomAdminCsvInput(
                uuid(input, "id"),
                text(input, "code"),
                text(input, "name"),
                integer(input, "defaultCapacity"),
                longVal(input, "baseDailyPriceVnd"),
                bool(input, "isActive"),
                uuid(input, "roomTypeId"),
                text(input, "roomCode"),
                integer(input, "floor"),
                integer(input, "capacity"),
                status(input, "statusCode")
        );
    }

    private RoomStatus status(String input, String key) {
        String value = text(input, key);
        return value == null ? null : RoomStatus.valueOf(value);
    }

    private Integer integer(String input, String key) {
        String value = text(input, key);
        return value == null ? null : Integer.valueOf(value);
    }

    private Long longVal(String input, String key) {
        String value = text(input, key);
        return value == null ? null : Long.valueOf(value);
    }

    private Boolean bool(String input, String key) {
        String value = text(input, key);
        return value == null ? null : Boolean.valueOf(value);
    }

    private UUID uuid(String input, String key) {
        String value = text(input, key);
        if (value == null) return null;
        if ("1".equals(value)) return UUID.fromString("00000000-0000-0000-0000-000000000001");
        if ("10".equals(value)) return UUID.fromString("00000000-0000-0000-0000-000000000010");
        return UUID.fromString("99999999-9999-9999-9999-999999999999");
    }

    private String text(String input, String key) {
        for (String part : input.split(";")) {
            String[] pair = part.trim().split("=", 2);
            if (pair.length == 2 && pair[0].trim().equals(key)) {
                String value = pair[1].trim();
                return value.isBlank() || "null".equalsIgnoreCase(value) ? null : value;
            }
        }
        return null;
    }

    private record RoomAdminCsvInput(
            UUID id,
            String code,
            String name,
            Integer defaultCapacity,
            Long baseDailyPriceVnd,
            Boolean isActive,
            UUID roomTypeId,
            String roomCode,
            Integer floor,
            Integer capacity,
            RoomStatus statusCode
    ) {
    }

    @org.junit.jupiter.api.Test
    void should_listRoomTypes_activeOnly() {
        given(roomTypeRepository.findByIsActiveTrueOrderByNameAsc()).willReturn(java.util.List.of(roomType(UUID.randomUUID())));
        given(roomMapper.toRoomTypeResponse(any())).willReturn(new RoomTypeResponse(null, null, null, null, null, null, null));
        
        var list = roomAdminService.listRoomTypes(true);
        assertThat(list).hasSize(1);
    }

    @org.junit.jupiter.api.Test
    void should_listRoomTypes_all() {
        given(roomTypeRepository.findAllByOrderByNameAsc()).willReturn(java.util.List.of(roomType(UUID.randomUUID()), roomType(UUID.randomUUID())));
        given(roomMapper.toRoomTypeResponse(any())).willReturn(new RoomTypeResponse(null, null, null, null, null, null, null));
        
        var list = roomAdminService.listRoomTypes(false);
        assertThat(list).hasSize(2);
    }

    @org.junit.jupiter.api.Test
    void should_listActiveRoomTypes() {
        given(roomTypeRepository.findByIsActiveTrueOrderByNameAsc()).willReturn(java.util.List.of(roomType(UUID.randomUUID())));
        given(roomMapper.toRoomTypeResponse(any())).willReturn(new RoomTypeResponse(null, null, null, null, null, null, null));
        
        var list = roomAdminService.listActiveRoomTypes();
        assertThat(list).hasSize(1);
    }

    @org.junit.jupiter.api.Test
    void should_getRoomType() {
        UUID id = UUID.randomUUID();
        given(roomTypeRepository.findById(id)).willReturn(Optional.of(roomType(id)));
        given(roomMapper.toRoomTypeResponse(any())).willReturn(new RoomTypeResponse(null, null, null, null, null, null, null));
        
        var type = roomAdminService.getRoomType(id);
        assertThat(type).isNotNull();
    }

    @org.junit.jupiter.api.Test
    void should_listRooms() {
        org.springframework.data.domain.PageRequest pageable = org.springframework.data.domain.PageRequest.of(0, 20);
        given(roomRepository.findAll(pageable)).willReturn(new org.springframework.data.domain.PageImpl<>(java.util.List.of(new Room()), pageable, 1));
        given(roomMapper.toRoomResponse(any())).willReturn(new RoomResponse(null, null, null, null, null, null, null, null, null));
        
        var page = roomAdminService.listRooms(pageable);
        assertThat(page.data().content()).hasSize(1);
    }

    @org.junit.jupiter.api.Test
    void should_generateRoomTypeCode_when_CodeIsNull() {
        RoomTypeRequest request = new RoomTypeRequest(null, "VIP Room!@#", 2, 100000L, null, true);
        RoomType saved = new RoomType();
        given(roomTypeRepository.existsByCodeAndIsActiveTrue("RT-VIPROOM-0001")).willReturn(false);
        given(roomMapper.toRoomType(request)).willReturn(saved);
        given(roomTypeRepository.save(any())).willReturn(saved);
        given(roomMapper.toRoomTypeResponse(any())).willReturn(new RoomTypeResponse(null, "RT-VIPROOM-0001", null, null, null, null, null));
        
        RoomTypeResponse response = roomAdminService.createRoomType(request);
        assertThat(response.code()).isEqualTo("RT-VIPROOM-0001");
    }

    @org.junit.jupiter.api.Test
    void should_generateRoomTypeCode_when_NameIsLong() {
        RoomTypeRequest request = new RoomTypeRequest(null, "VeryLongNameIndeedYes", 2, 100000L, null, true);
        RoomType saved = new RoomType();
        given(roomTypeRepository.existsByCodeAndIsActiveTrue("RT-VERYLONGNA-0001")).willReturn(false);
        given(roomMapper.toRoomType(request)).willReturn(saved);
        given(roomTypeRepository.save(any())).willReturn(saved);
        given(roomMapper.toRoomTypeResponse(any())).willReturn(new RoomTypeResponse(null, "RT-VERYLONGNA-0001", null, null, null, null, null));
        
        RoomTypeResponse response = roomAdminService.createRoomType(request);
        assertThat(response.code()).isEqualTo("RT-VERYLONGNA-0001");
    }

    @org.junit.jupiter.api.Test
    void should_generateRoomTypeCode_when_NameIsBlankSymbols() {
        RoomTypeRequest request = new RoomTypeRequest("", "!@#", 2, 100000L, null, true);
        RoomType saved = new RoomType();
        given(roomTypeRepository.existsByCodeAndIsActiveTrue("RT-ROOMTYPE-0001")).willReturn(false);
        given(roomMapper.toRoomType(request)).willReturn(saved);
        given(roomTypeRepository.save(any())).willReturn(saved);
        given(roomMapper.toRoomTypeResponse(any())).willReturn(new RoomTypeResponse(null, "RT-ROOMTYPE-0001", null, null, null, null, null));
        
        RoomTypeResponse response = roomAdminService.createRoomType(request);
        assertThat(response.code()).isEqualTo("RT-ROOMTYPE-0001");
    }

    @org.junit.jupiter.api.Test
    void should_generateRoomTypeCode_when_CodeAlreadyExistsSequence() {
        RoomTypeRequest request = new RoomTypeRequest(null, "VIP", 2, 100000L, null, true);
        RoomType saved = new RoomType();
        given(roomTypeRepository.existsByCodeAndIsActiveTrue("RT-VIP-0001")).willReturn(true);
        given(roomTypeRepository.existsByCodeAndIsActiveTrue("RT-VIP-0002")).willReturn(false);
        given(roomMapper.toRoomType(request)).willReturn(saved);
        given(roomTypeRepository.save(any())).willReturn(saved);
        given(roomMapper.toRoomTypeResponse(any())).willReturn(new RoomTypeResponse(null, "RT-VIP-0002", null, null, null, null, null));
        
        RoomTypeResponse response = roomAdminService.createRoomType(request);
        assertThat(response.code()).isEqualTo("RT-VIP-0002");
    }

    @org.junit.jupiter.api.Test
    void should_resolveRoomTypeCode_when_UpdateWithExistingId() {
        UUID id = UUID.randomUUID();
        RoomTypeRequest request = new RoomTypeRequest(null, "VIP", 2, 100000L, null, true);
        RoomType roomType = new RoomType();
        roomType.setId(id);
        roomType.setCode("EXISTING");
        
        given(roomTypeRepository.findById(id)).willReturn(Optional.of(roomType));
        given(roomTypeRepository.existsByCodeAndIdNot("EXISTING", id)).willReturn(false);
        given(roomTypeRepository.save(any())).willReturn(roomType);
        given(roomMapper.toRoomTypeResponse(any())).willReturn(new RoomTypeResponse(id, "EXISTING", null, null, null, null, null));
        
        RoomTypeResponse response = roomAdminService.updateRoomType(id, request);
        assertThat(response.code()).isEqualTo("EXISTING");
    }


    @org.junit.jupiter.api.Test
    void createRoomType_shouldHandleNullAndFalseIsActive() {
        // Null isActive
        RoomTypeRequest req1 = new RoomTypeRequest("CODE1", "Name1", 2, 1000L, null, null);
        RoomType rt1 = new RoomType();
        given(roomMapper.toRoomType(req1)).willReturn(rt1);
        given(roomTypeRepository.save(any())).willReturn(rt1);
        given(roomMapper.toRoomTypeResponse(any())).willReturn(new RoomTypeResponse(null, null, null, null, null, null, null));
        roomAdminService.createRoomType(req1);
        assertThat(rt1.getIsActive()).isTrue();
        
        // False isActive
        RoomTypeRequest req2 = new RoomTypeRequest("CODE2", "Name2", 2, 1000L, null, false);
        RoomType rt2 = new RoomType();
        given(roomMapper.toRoomType(req2)).willReturn(rt2);
        given(roomTypeRepository.save(any())).willReturn(rt2);
        given(roomMapper.toRoomTypeResponse(any())).willReturn(new RoomTypeResponse(null, null, null, null, null, null, null));
        roomAdminService.createRoomType(req2);
        assertThat(rt2.getIsActive()).isFalse();
    }

    @org.junit.jupiter.api.Test
    void validateRoomTypeRequest_shouldThrow_whenValuesInvalid() {
        RoomTypeRequest req1 = new RoomTypeRequest("C", "N", 0, 1000L, null, true);
        assertThatThrownBy(() -> roomAdminService.createRoomType(req1))
                .isInstanceOf(BusinessException.class).hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_VALIDATION_FAILED);

        RoomTypeRequest req2 = new RoomTypeRequest("C", "N", 1, -1L, null, true);
        assertThatThrownBy(() -> roomAdminService.createRoomType(req2))
                .isInstanceOf(BusinessException.class).hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_VALIDATION_FAILED);
    }

    @org.junit.jupiter.api.Test
    void validateRoomRequest_shouldThrow_whenValuesInvalid() {
        UUID rtId = UUID.randomUUID();
        // capacity null
        RoomRequest req1 = new RoomRequest("C", "N", rtId, 1, null, RoomStatus.AVAILABLE, "");
        assertThatThrownBy(() -> roomAdminService.createRoom(req1)).isInstanceOf(BusinessException.class);
        
        // capacity < 1
        RoomRequest req2 = new RoomRequest("C", "N", rtId, 1, 0, RoomStatus.AVAILABLE, "");
        assertThatThrownBy(() -> roomAdminService.createRoom(req2)).isInstanceOf(BusinessException.class);

        // floor null
        RoomRequest req3 = new RoomRequest("C", "N", rtId, null, 1, RoomStatus.AVAILABLE, "");
        assertThatThrownBy(() -> roomAdminService.createRoom(req3)).isInstanceOf(BusinessException.class);

        // floor < 1
        RoomRequest req4 = new RoomRequest("C", "N", rtId, 0, 1, RoomStatus.AVAILABLE, "");
        assertThatThrownBy(() -> roomAdminService.createRoom(req4)).isInstanceOf(BusinessException.class);
    }

    @org.junit.jupiter.api.Test
    void updateRoom_shouldThrow_whenRoomNotFound() {
        UUID roomId = UUID.randomUUID();
        RoomRequest req = new RoomRequest("C", "N", UUID.randomUUID(), 1, 1, RoomStatus.AVAILABLE, "");
        given(roomRepository.findWithRoomTypeById(roomId)).willReturn(Optional.empty());
        assertThatThrownBy(() -> roomAdminService.updateRoom(roomId, req))
                .isInstanceOf(BusinessException.class).hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_ROOM_002_ROOM_NOT_FOUND);
    }

    @org.junit.jupiter.api.Test
    void updateRoom_shouldThrow_whenRoomTypeNotFound() {
        UUID roomId = UUID.randomUUID();
        UUID rtId = UUID.randomUUID();
        RoomRequest req = new RoomRequest("C", "N", rtId, 1, 1, RoomStatus.AVAILABLE, "");
        given(roomRepository.findWithRoomTypeById(roomId)).willReturn(Optional.of(new Room()));
        given(roomTypeRepository.findByIdAndIsActiveTrue(rtId)).willReturn(Optional.empty());
        assertThatThrownBy(() -> roomAdminService.updateRoom(roomId, req))
                .isInstanceOf(BusinessException.class).hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_ROOM_001_ROOM_TYPE_NOT_FOUND);
    }

    @org.junit.jupiter.api.Test
    void updateRoomStatus_shouldThrow_whenRoomNotFound() {
        UUID roomId = UUID.randomUUID();
        RoomStatusUpdateRequest req = new RoomStatusUpdateRequest(RoomStatus.MAINTENANCE);
        given(roomRepository.findWithRoomTypeById(roomId)).willReturn(Optional.empty());
        assertThatThrownBy(() -> roomAdminService.updateRoomStatus(roomId, req))
                .isInstanceOf(BusinessException.class).hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_ROOM_002_ROOM_NOT_FOUND);
    }

    @org.junit.jupiter.api.Test
    void findRoomType_shouldThrow_whenNotFound() {
        UUID rtId = UUID.randomUUID();
        given(roomTypeRepository.findById(rtId)).willReturn(Optional.empty());
        RoomTypeRequest req = new RoomTypeRequest("C", "N", 1, 1000L, null, true);
        assertThatThrownBy(() -> roomAdminService.updateRoomType(rtId, req))
                .isInstanceOf(BusinessException.class).hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_ROOM_001_ROOM_TYPE_NOT_FOUND);
    }

    @org.junit.jupiter.api.Test
    void resolveRoomTypeCode_shouldThrow_whenAllCodesExist() {
        RoomTypeRequest request = new RoomTypeRequest(null, "VIP", 2, 100000L, null, true);
        // mock existsByCodeAndIsActiveTrue to return true for all ANY
        given(roomTypeRepository.existsByCodeAndIsActiveTrue(any())).willReturn(true);
        assertThatThrownBy(() -> roomAdminService.createRoomType(request))
                .isInstanceOf(BusinessException.class).hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_ROOM_005_TYPE_CODE_EXISTS);
    }

}
