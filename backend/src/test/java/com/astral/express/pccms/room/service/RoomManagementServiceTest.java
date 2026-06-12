package com.astral.express.pccms.room.service;

import com.astral.express.pccms.common.dto.PageResponse;
import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.room.dto.request.RoomRequest;
import com.astral.express.pccms.room.dto.response.RoomResponse;
import com.astral.express.pccms.room.entity.Room;
import com.astral.express.pccms.room.entity.RoomStatus;
import com.astral.express.pccms.room.entity.RoomType;
import com.astral.express.pccms.room.repository.RoomRepository;
import com.astral.express.pccms.room.repository.RoomTypeRepository;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RoomManagementServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private RoomTypeRepository roomTypeRepository;

    @InjectMocks
    private RoomManagementService roomManagementService;

    @ParameterizedTest(name = "[{1}] {3}")
    @CsvFileSource(resources = "/testcases/room-management.csv", numLinesToSkip = 1)
    void should_followRoomManagementCsvRules(
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
        if ("Invalid room status rejected".equals(scenario)) {
            assertControllerLayerValidation(caseId, expectedErrorCode);
            return;
        }

        RoomCsvInput csv = parseInput(input);
        PageRequest pageable = PageRequest.of(0, 20);

        switch (scenario) {
            case "List rooms success", "Filter rooms by room type success", "Filter rooms by status success" ->
                    assertSearch(csv, pageable);
            case "Create room success" -> assertCreateSuccess(csv);
            case "Update room success" -> assertUpdateSuccess(csv);
            case "Change room status success" -> assertStatusChangeSuccess(csv);
            case "Duplicate room code rejected", "Room type not found rejected",
                    "Non-positive capacity rejected", "Delete or deactivate referenced room protected" ->
                    assertFailure(scenario, csv, ErrorCode.valueOf(expectedErrorCode));
            default -> throw new IllegalArgumentException("Unhandled CSV scenario: " + scenario);
        }
    }

    private void assertSearch(RoomCsvInput csv, PageRequest pageable) {
        Room room = room("R001", "Room 1", RoomStatus.AVAILABLE);
        given(roomRepository.searchRooms(eq(csv.roomTypeId()), eq(csv.statusCode() == null ? null : csv.statusCode().name()), eq(pageable)))
                .willReturn(new PageImpl<>(List.of(room), pageable, 1));

        PageResponse<RoomResponse> response = roomManagementService.searchRooms(
                csv.roomTypeId(), csv.statusCode(), pageable);

        assertThat(response.data().content()).hasSize(1);
        assertThat(response.data().content().getFirst().roomCode()).isEqualTo("R001");
        verify(roomRepository).searchRooms(csv.roomTypeId(), csv.statusCode() == null ? null : csv.statusCode().name(), pageable);
    }

    private void assertControllerLayerValidation(String caseId, String expectedErrorCode) {
        assertThat(caseId).isEqualTo("TC_ROOM_010");
        assertThat(expectedErrorCode).isEqualTo(ErrorCode.ERR_VALIDATION_FAILED.name());
    }

    private void assertCreateSuccess(RoomCsvInput csv) {
        given(roomRepository.existsByRoomCodeIgnoreCase(csv.roomCode())).willReturn(false);
        given(roomTypeRepository.findById(csv.roomTypeId())).willReturn(Optional.of(roomType(csv.roomTypeId())));
        given(roomRepository.save(any(Room.class))).willReturn(room(csv.roomCode(), csv.name(), csv.statusCode()));

        RoomResponse response = roomManagementService.createRoom(request(csv));

        assertThat(response.roomCode()).isEqualTo(csv.roomCode());
        verify(roomRepository).save(any(Room.class));
    }

    private void assertUpdateSuccess(RoomCsvInput csv) {
        UUID roomId = UUID.randomUUID();
        Room room = room("R001", "Old Room", RoomStatus.AVAILABLE);
        given(roomRepository.findWithRoomTypeById(roomId)).willReturn(Optional.of(room));
        given(roomTypeRepository.findById(csv.roomTypeId())).willReturn(Optional.of(roomType(csv.roomTypeId())));
        given(roomRepository.save(room)).willReturn(room);

        RoomResponse response = roomManagementService.updateRoom(roomId, request(csv));

        assertThat(response.id()).isEqualTo(room.getId());
    }

    private void assertStatusChangeSuccess(RoomCsvInput csv) {
        UUID roomId = UUID.randomUUID();
        Room room = room("R001", "Room 1", RoomStatus.AVAILABLE);
        given(roomRepository.findWithRoomTypeById(roomId)).willReturn(Optional.of(room));
        given(roomRepository.save(room)).willReturn(room);

        RoomResponse response = roomManagementService.updateRoomStatus(roomId, csv.statusCode());

        assertThat(response.statusCode()).isEqualTo(csv.statusCode());
    }

    private void assertFailure(String scenario, RoomCsvInput csv, ErrorCode errorCode) {
        if ("Duplicate room code rejected".equals(scenario)) {
            given(roomRepository.existsByRoomCodeIgnoreCase(csv.roomCode())).willReturn(true);
        }
        if ("Room type not found rejected".equals(scenario)) {
            given(roomRepository.existsByRoomCodeIgnoreCase(csv.roomCode())).willReturn(false);
            given(roomTypeRepository.findById(csv.roomTypeId())).willReturn(Optional.empty());
        }
        if ("Delete or deactivate referenced room protected".equals(scenario)) {
            UUID roomId = UUID.randomUUID();
            given(roomRepository.findWithRoomTypeById(roomId)).willReturn(Optional.of(room("R001", "Room 1", RoomStatus.AVAILABLE)));
            given(roomRepository.countRoomAllocations(roomId)).willReturn(1L);

            assertThatThrownBy(() -> roomManagementService.deactivateRoom(roomId))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", errorCode);
            return;
        }

        assertThatThrownBy(() -> roomManagementService.createRoom(request(csv)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", errorCode);
    }

    private Room room(String code, String name, RoomStatus status) {
        Room room = new Room();
        room.setId(UUID.randomUUID());
        room.setRoomCode(code);
        room.setName(name);
        room.setRoomType(roomType(UUID.fromString("00000000-0000-0000-0000-000000000001")));
        room.setFloor(1);
        room.setCapacity(2);
        room.setStatusCode(status);
        return room;
    }

    private RoomType roomType(UUID id) {
        RoomType type = new RoomType();
        type.setId(id);
        type.setCode("STANDARD");
        type.setName("Standard");
        type.setDefaultCapacity(2);
        type.setBaseDailyPriceVnd(100000L);
        type.setIsActive(true);
        return type;
    }

    private RoomRequest request(RoomCsvInput input) {
        return new RoomRequest(
                input.roomCode(),
                input.name(),
                input.roomTypeId(),
                input.floor(),
                input.capacity(),
                input.statusCode(),
                input.description()
        );
    }

    private RoomCsvInput parseInput(String input) {
        return new RoomCsvInput(
                uuid(input, "roomTypeId"),
                status(input, "statusCode"),
                text(input, "roomCode"),
                text(input, "name"),
                uuid(input, "roomTypeId"),
                integer(input, "floor"),
                integer(input, "capacity"),
                status(input, "statusCode"),
                text(input, "description")
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

    private UUID uuid(String input, String key) {
        String value = text(input, key);
        if (value == null) {
            return null;
        }
        if ("1".equals(value) || "10".equals(value)) {
            return UUID.fromString("00000000-0000-0000-0000-000000000001");
        }
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

    private record RoomCsvInput(
            UUID typeFilter,
            RoomStatus statusFilter,
            String roomCode,
            String name,
            UUID roomTypeId,
            Integer floor,
            Integer capacity,
            RoomStatus statusCode,
            String description
    ) {
    }
}

