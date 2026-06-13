package com.astral.express.pccms.room.service;

import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.room.dto.request.RoomRequest;
import com.astral.express.pccms.room.dto.response.RoomResponse;
import com.astral.express.pccms.room.entity.Room;
import com.astral.express.pccms.room.entity.RoomStatus;
import com.astral.express.pccms.room.entity.RoomType;
import com.astral.express.pccms.room.repository.RoomRepository;
import com.astral.express.pccms.room.repository.RoomTypeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RoomManagementServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private RoomTypeRepository roomTypeRepository;

    @InjectMocks
    private RoomManagementService roomManagementService;

    @Test
    void searchRooms_shouldReturnPage() {
        PageRequest pageable = PageRequest.of(0, 10);
        UUID typeId = UUID.randomUUID();
        Room room = room(UUID.randomUUID(), roomType(typeId));
        given(roomRepository.searchRooms(typeId, "AVAILABLE", pageable))
                .willReturn(new PageImpl<>(List.of(room), pageable, 1));

        var response = roomManagementService.searchRooms(typeId, RoomStatus.AVAILABLE, pageable);

        assertThat(response.data().content()).hasSize(1);
    }

    @Test
    void getRoom_shouldReturnRoom_whenFound() {
        UUID roomId = UUID.randomUUID();
        Room room = room(roomId, roomType(UUID.randomUUID()));
        given(roomRepository.findWithRoomTypeById(roomId)).willReturn(Optional.of(room));

        RoomResponse response = roomManagementService.getRoom(roomId);

        assertThat(response.id()).isEqualTo(roomId);
    }

    @Test
    void getRoom_shouldThrowException_whenNotFound() {
        UUID roomId = UUID.randomUUID();
        given(roomRepository.findWithRoomTypeById(roomId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> roomManagementService.getRoom(roomId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_404_NOT_FOUND);
    }

    @Test
    void createRoom_shouldThrowException_whenCapacityIsInvalid() {
        RoomRequest request = new RoomRequest("R1", "Room 1", UUID.randomUUID(), 1, 0, RoomStatus.AVAILABLE, "");

        assertThatThrownBy(() -> roomManagementService.createRoom(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_VALIDATION_FAILED);
    }

    @Test
    void createRoom_shouldThrowException_whenRoomCodeExists() {
        RoomRequest request = new RoomRequest("R1", "Room 1", UUID.randomUUID(), 1, 10, RoomStatus.AVAILABLE, "");
        given(roomRepository.existsByRoomCodeIgnoreCase("R1")).willReturn(true);

        assertThatThrownBy(() -> roomManagementService.createRoom(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_VALIDATION_FAILED);
    }

    @Test
    void createRoom_shouldThrowException_whenRoomTypeNotActive() {
        UUID typeId = UUID.randomUUID();
        RoomRequest request = new RoomRequest("R1", "Room 1", typeId, 1, 10, RoomStatus.AVAILABLE, "");
        given(roomRepository.existsByRoomCodeIgnoreCase("R1")).willReturn(false);
        RoomType roomType = roomType(typeId);
        roomType.setIsActive(false);
        given(roomTypeRepository.findById(typeId)).willReturn(Optional.of(roomType));

        assertThatThrownBy(() -> roomManagementService.createRoom(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_VALIDATION_FAILED);
    }

    @Test
    void createRoom_shouldCreateRoom() {
        UUID typeId = UUID.randomUUID();
        RoomRequest request = new RoomRequest("R1", "Room 1", typeId, 1, 10, RoomStatus.AVAILABLE, "");
        given(roomRepository.existsByRoomCodeIgnoreCase("R1")).willReturn(false);
        RoomType roomType = roomType(typeId);
        given(roomTypeRepository.findById(typeId)).willReturn(Optional.of(roomType));

        Room roomToSave = room(UUID.randomUUID(), roomType);
        roomToSave.setRoomCode("R1");
        given(roomRepository.save(any(Room.class))).willReturn(roomToSave);

        RoomResponse response = roomManagementService.createRoom(request);

        assertThat(response.roomCode()).isEqualTo("R1");
        verify(roomRepository).save(any(Room.class));
    }

    @Test
    void createRoom_shouldCreateRoomWithGeneratedCode() {
        UUID typeId = UUID.randomUUID();
        RoomRequest request = new RoomRequest("", "Room 1", typeId, 1, 10, RoomStatus.AVAILABLE, "");
        RoomType roomType = roomType(typeId);
        given(roomTypeRepository.findById(typeId)).willReturn(Optional.of(roomType));

        Room roomToSave = room(UUID.randomUUID(), roomType);
        roomToSave.setRoomCode("ROOM20231010121212");
        given(roomRepository.save(any(Room.class))).willReturn(roomToSave);

        RoomResponse response = roomManagementService.createRoom(request);

        assertThat(response.roomCode()).startsWith("ROOM");
    }

    @Test
    void updateRoom_shouldThrowException_whenRoomCodeExistsForOtherRoom() {
        UUID roomId = UUID.randomUUID();
        RoomRequest request = new RoomRequest("R1", "Room 1", UUID.randomUUID(), 1, 10, RoomStatus.AVAILABLE, "");
        given(roomRepository.findWithRoomTypeById(roomId)).willReturn(Optional.of(room(roomId, roomType(UUID.randomUUID()))));
        given(roomRepository.existsByRoomCodeIgnoreCaseAndIdNot("R1", roomId)).willReturn(true);

        assertThatThrownBy(() -> roomManagementService.updateRoom(roomId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_VALIDATION_FAILED);
    }

    @Test
    void updateRoom_shouldUpdateRoom() {
        UUID roomId = UUID.randomUUID();
        UUID typeId = UUID.randomUUID();
        RoomRequest request = new RoomRequest("R1", "Room 1", typeId, 1, 10, RoomStatus.AVAILABLE, "");
        Room room = room(roomId, roomType(typeId));
        given(roomRepository.findWithRoomTypeById(roomId)).willReturn(Optional.of(room));
        given(roomRepository.existsByRoomCodeIgnoreCaseAndIdNot("R1", roomId)).willReturn(false);
        given(roomTypeRepository.findById(typeId)).willReturn(Optional.of(roomType(typeId)));
        given(roomRepository.save(any(Room.class))).willReturn(room);

        RoomResponse response = roomManagementService.updateRoom(roomId, request);

        assertThat(response.id()).isEqualTo(roomId);
        verify(roomRepository).save(room);
    }

    @Test
    void updateRoomStatus_shouldUpdateStatus() {
        UUID roomId = UUID.randomUUID();
        Room room = room(roomId, roomType(UUID.randomUUID()));
        given(roomRepository.findWithRoomTypeById(roomId)).willReturn(Optional.of(room));
        given(roomRepository.save(any(Room.class))).willReturn(room);

        RoomResponse response = roomManagementService.updateRoomStatus(roomId, RoomStatus.INACTIVE);

        assertThat(response.statusCode()).isEqualTo(RoomStatus.INACTIVE);
        assertThat(room.getStatusCode()).isEqualTo(RoomStatus.INACTIVE);
    }

    @Test
    void deactivateRoom_shouldThrowException_whenRoomHasAllocations() {
        UUID roomId = UUID.randomUUID();
        Room room = room(roomId, roomType(UUID.randomUUID()));
        given(roomRepository.findWithRoomTypeById(roomId)).willReturn(Optional.of(room));
        given(roomRepository.countRoomAllocations(roomId)).willReturn(1L);

        assertThatThrownBy(() -> roomManagementService.deactivateRoom(roomId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_VALIDATION_FAILED);
    }

    @Test
    void deactivateRoom_shouldDeactivate() {
        UUID roomId = UUID.randomUUID();
        Room room = room(roomId, roomType(UUID.randomUUID()));
        given(roomRepository.findWithRoomTypeById(roomId)).willReturn(Optional.of(room));
        given(roomRepository.countRoomAllocations(roomId)).willReturn(0L);
        given(roomRepository.save(any(Room.class))).willReturn(room);

        RoomResponse response = roomManagementService.deactivateRoom(roomId);

        assertThat(response.statusCode()).isEqualTo(RoomStatus.INACTIVE);
        assertThat(room.getStatusCode()).isEqualTo(RoomStatus.INACTIVE);
    }

    private Room room(UUID id, RoomType type) {
        Room room = new Room();
        room.setId(id);
        room.setRoomCode("RC" + id.toString().substring(0, 4));
        room.setName("Name");
        room.setRoomType(type);
        room.setCapacity(10);
        room.setStatusCode(RoomStatus.AVAILABLE);
        room.setFloor(1);
        return room;
    }

    private RoomType roomType(UUID id) {
        RoomType roomType = new RoomType();
        roomType.setId(id);
        roomType.setName("Type Name");
        roomType.setIsActive(true);
        return roomType;
    }

    @Test
    void searchRooms_shouldReturnPage_withNullStatus() {
        PageRequest pageable = PageRequest.of(0, 10);
        UUID typeId = UUID.randomUUID();
        Room room = room(UUID.randomUUID(), roomType(typeId));
        given(roomRepository.searchRooms(typeId, null, pageable))
                .willReturn(new PageImpl<>(List.of(room), pageable, 1));

        var response = roomManagementService.searchRooms(typeId, null, pageable);

        assertThat(response.data().content()).hasSize(1);
    }

    @Test
    void updateRoom_shouldUpdateRoomWithNullCode() {
        UUID roomId = UUID.randomUUID();
        UUID typeId = UUID.randomUUID();
        RoomRequest request = new RoomRequest(null, "Room 1", typeId, 1, 10, RoomStatus.AVAILABLE, "");
        Room room = room(roomId, roomType(typeId));
        given(roomRepository.findWithRoomTypeById(roomId)).willReturn(Optional.of(room));
        given(roomTypeRepository.findById(typeId)).willReturn(Optional.of(roomType(typeId)));
        given(roomRepository.save(any(Room.class))).willReturn(room);

        RoomResponse response = roomManagementService.updateRoom(roomId, request);

        assertThat(response.id()).isEqualTo(roomId);
        verify(roomRepository).save(room);
    }

    @Test
    void getRoom_shouldReturnRoomResponse_withNullRoomType() {
        UUID roomId = UUID.randomUUID();
        Room room = room(roomId, null);
        given(roomRepository.findWithRoomTypeById(roomId)).willReturn(Optional.of(room));

        RoomResponse response = roomManagementService.getRoom(roomId);

        assertThat(response.id()).isEqualTo(roomId);
        assertThat(response.roomTypeId()).isNull();
        assertThat(response.roomTypeName()).isNull();
    }

}
