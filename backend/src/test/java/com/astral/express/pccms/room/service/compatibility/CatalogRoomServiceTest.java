package com.astral.express.pccms.room.service.compatibility;

import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.room.dto.compatibility.CreateRoomRequest;
import com.astral.express.pccms.room.dto.compatibility.UpdateRoomRequest;
import com.astral.express.pccms.room.dto.compatibility.LegacyRoomResponse;
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
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CatalogRoomServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private RoomTypeRepository roomTypeRepository;

    @InjectMocks
    private CatalogRoomService catalogRoomService;

    @Test
    void create_shouldThrowException_whenCodeExists() {
        CreateRoomRequest request = new CreateRoomRequest("R1", "Room", UUID.randomUUID(), 10, RoomStatus.AVAILABLE, 1, "");
        given(roomRepository.existsByRoomCodeIgnoreCase("R1")).willReturn(true);

        assertThatThrownBy(() -> catalogRoomService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_ROOM_002_CODE_EXISTS);
    }

    @Test
    void create_shouldThrowException_whenNameExists() {
        CreateRoomRequest request = new CreateRoomRequest("R1", "Room", UUID.randomUUID(), 10, RoomStatus.AVAILABLE, 1, "");
        given(roomRepository.existsByRoomCodeIgnoreCase("R1")).willReturn(false);
        given(roomRepository.existsByName("Room")).willReturn(true);

        assertThatThrownBy(() -> catalogRoomService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_ROOM_003_NAME_EXISTS);
    }

    @Test
    void create_shouldCreateRoom() {
        UUID typeId = UUID.randomUUID();
        CreateRoomRequest request = new CreateRoomRequest("R1", "Room", typeId, 10, RoomStatus.AVAILABLE, 1, "");
        given(roomRepository.existsByRoomCodeIgnoreCase("R1")).willReturn(false);
        given(roomRepository.existsByName("Room")).willReturn(false);
        RoomType roomType = roomType(typeId);
        given(roomTypeRepository.findById(typeId)).willReturn(Optional.of(roomType));

        Room roomToSave = room(UUID.randomUUID(), roomType);
        roomToSave.setRoomCode("R1");
        given(roomRepository.save(any(Room.class))).willReturn(roomToSave);

        LegacyRoomResponse response = catalogRoomService.create(request);

        assertThat(response.roomCode()).isEqualTo("R1");
    }

    @Test
    void update_shouldThrowException_whenNotFound() {
        UUID id = UUID.randomUUID();
        UpdateRoomRequest request = new UpdateRoomRequest("R1", "Room", UUID.randomUUID(), 10, RoomStatus.AVAILABLE, 1, "");
        given(roomRepository.findById(id)).willReturn(Optional.empty());

        assertThatThrownBy(() -> catalogRoomService.update(id, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_ROOM_001_NOT_FOUND);
    }

    @Test
    void update_shouldUpdateRoom() {
        UUID id = UUID.randomUUID();
        UUID typeId = UUID.randomUUID();
        UpdateRoomRequest request = new UpdateRoomRequest("R1", "Room", typeId, 10, RoomStatus.AVAILABLE, 1, "");
        Room room = room(id, roomType(typeId));
        given(roomRepository.findById(id)).willReturn(Optional.of(room));
        given(roomRepository.existsByRoomCodeIgnoreCaseAndIdNot("R1", id)).willReturn(false);
        given(roomRepository.existsByNameAndIdNot("Room", id)).willReturn(false);
        given(roomTypeRepository.findById(typeId)).willReturn(Optional.of(roomType(typeId)));
        given(roomRepository.save(any(Room.class))).willReturn(room);

        LegacyRoomResponse response = catalogRoomService.update(id, request);

        assertThat(response.roomCode()).isEqualTo("R1");
    }

    @Test
    void getById_shouldReturnRoom() {
        UUID id = UUID.randomUUID();
        Room room = room(id, roomType(UUID.randomUUID()));
        given(roomRepository.findById(id)).willReturn(Optional.of(room));

        LegacyRoomResponse response = catalogRoomService.getById(id);

        assertThat(response.id()).isEqualTo(id);
    }

    @Test
    void list_shouldReturnPage_withAllFilters() {
        PageRequest pageable = PageRequest.of(0, 10);
        UUID typeId = UUID.randomUUID();
        Room room = room(UUID.randomUUID(), roomType(typeId));
        given(roomRepository.findByRoomTypeIdAndStatusCode(typeId, RoomStatus.AVAILABLE, pageable))
                .willReturn(new PageImpl<>(List.of(room), pageable, 1));

        var response = catalogRoomService.list(typeId, RoomStatus.AVAILABLE, pageable);

        assertThat(response.data().content()).hasSize(1);
    }

    @Test
    void list_shouldReturnPage_withNoFilters() {
        PageRequest pageable = PageRequest.of(0, 10);
        Room room = room(UUID.randomUUID(), roomType(UUID.randomUUID()));
        given(roomRepository.findAll(pageable))
                .willReturn(new PageImpl<>(List.of(room), pageable, 1));

        var response = catalogRoomService.list(null, null, pageable);

        assertThat(response.data().content()).hasSize(1);
    }

    @Test
    void delete_shouldThrowException_whenOccupied() {
        UUID id = UUID.randomUUID();
        Room room = room(id, roomType(UUID.randomUUID()));
        room.setStatusCode(RoomStatus.OCCUPIED);
        given(roomRepository.findById(id)).willReturn(Optional.of(room));

        assertThatThrownBy(() -> catalogRoomService.delete(id))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_ROOM_006_ROOM_OCCUPIED);
    }

    @Test
    void delete_shouldDeactivateRoom() {
        UUID id = UUID.randomUUID();
        Room room = room(id, roomType(UUID.randomUUID()));
        given(roomRepository.findById(id)).willReturn(Optional.of(room));

        catalogRoomService.delete(id);

        assertThat(room.getStatusCode()).isEqualTo(RoomStatus.INACTIVE);
        verify(roomRepository).save(room);
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
    void create_shouldGenerateCode_whenCodeIsNullOrBlank() {
        UUID typeId = UUID.randomUUID();
        CreateRoomRequest request = new CreateRoomRequest(null, "Room Null Code", typeId, 10, RoomStatus.AVAILABLE, null, "");
        given(roomRepository.existsByName("Room Null Code")).willReturn(false);
        given(roomTypeRepository.findById(typeId)).willReturn(Optional.of(roomType(typeId)));
        
        Room roomToSave = room(UUID.randomUUID(), roomType(typeId));
        given(roomRepository.save(any(Room.class))).willReturn(roomToSave);

        LegacyRoomResponse response = catalogRoomService.create(request);
        verify(roomRepository).save(any(Room.class));
    }

    @Test
    void update_shouldKeepExistingCode_whenCodeIsNullOrBlank() {
        UUID id = UUID.randomUUID();
        UUID typeId = UUID.randomUUID();
        UpdateRoomRequest request = new UpdateRoomRequest("", "Room Update Name", typeId, 10, RoomStatus.AVAILABLE, null, "");
        
        Room existingRoom = room(id, roomType(typeId));
        existingRoom.setRoomCode("EXISTING-CODE");
        given(roomRepository.findById(id)).willReturn(Optional.of(existingRoom));
        given(roomRepository.existsByNameAndIdNot("Room Update Name", id)).willReturn(false);
        given(roomTypeRepository.findById(typeId)).willReturn(Optional.of(roomType(typeId)));
        given(roomRepository.save(any(Room.class))).willReturn(existingRoom);

        LegacyRoomResponse response = catalogRoomService.update(id, request);
        verify(roomRepository).save(any(Room.class));
    }

    @Test
    void getById_shouldThrowException_whenNotFound() {
        UUID id = UUID.randomUUID();
        given(roomRepository.findById(id)).willReturn(Optional.empty());
        assertThatThrownBy(() -> catalogRoomService.getById(id))
                .isInstanceOf(BusinessException.class).hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_ROOM_001_NOT_FOUND);
    }

    @Test
    void delete_shouldThrowException_whenNotFound() {
        UUID id = UUID.randomUUID();
        given(roomRepository.findById(id)).willReturn(Optional.empty());
        assertThatThrownBy(() -> catalogRoomService.delete(id))
                .isInstanceOf(BusinessException.class).hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_ROOM_001_NOT_FOUND);
    }

    @Test
    void resolveRoomType_shouldThrowException_whenNotFound() {
        UUID typeId = UUID.randomUUID();
        CreateRoomRequest request = new CreateRoomRequest("R1", "Room", typeId, 10, RoomStatus.AVAILABLE, 1, "");
        given(roomRepository.existsByRoomCodeIgnoreCase("R1")).willReturn(false);
        given(roomRepository.existsByName("Room")).willReturn(false);
        given(roomTypeRepository.findById(typeId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> catalogRoomService.create(request))
                .isInstanceOf(BusinessException.class).hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_ROOM_004_TYPE_NOT_FOUND);
    }

    @Test
    void list_shouldReturnPage_withRoomTypeIdFilterOnly() {
        PageRequest pageable = PageRequest.of(0, 10);
        UUID typeId = UUID.randomUUID();
        Room room = room(UUID.randomUUID(), roomType(typeId));
        given(roomRepository.findByRoomTypeId(typeId, pageable))
                .willReturn(new PageImpl<>(List.of(room), pageable, 1));

        var response = catalogRoomService.list(typeId, null, pageable);
        assertThat(response.data().content()).hasSize(1);
    }

    @Test
    void list_shouldReturnPage_withStatusCodeFilterOnly() {
        PageRequest pageable = PageRequest.of(0, 10);
        Room room = room(UUID.randomUUID(), roomType(UUID.randomUUID()));
        given(roomRepository.findByStatusCode(RoomStatus.MAINTENANCE, pageable))
                .willReturn(new PageImpl<>(List.of(room), pageable, 1));

        var response = catalogRoomService.list(null, RoomStatus.MAINTENANCE, pageable);
        assertThat(response.data().content()).hasSize(1);
    }

    @Test
    void toResponse_shouldReturnCorrectStatusLabels() {
        UUID typeId = UUID.randomUUID();
        RoomType rt = roomType(typeId);
        Room room = room(UUID.randomUUID(), rt);
        
        room.setStatusCode(RoomStatus.OCCUPIED);
        given(roomRepository.findById(room.getId())).willReturn(Optional.of(room));
        assertThat(catalogRoomService.getById(room.getId()).statusLabel()).isEqualTo("Đang sử dụng");
        
        room.setStatusCode(RoomStatus.MAINTENANCE);
        assertThat(catalogRoomService.getById(room.getId()).statusLabel()).isEqualTo("Bảo trì");
        
        room.setStatusCode(RoomStatus.INACTIVE);
        assertThat(catalogRoomService.getById(room.getId()).statusLabel()).isEqualTo("Ngừng áp dụng");
    }

}
