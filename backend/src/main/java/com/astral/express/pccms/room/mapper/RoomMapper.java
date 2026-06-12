package com.astral.express.pccms.room.mapper;

import com.astral.express.pccms.room.dto.request.RoomRequest;
import com.astral.express.pccms.room.dto.request.RoomTypeRequest;
import com.astral.express.pccms.room.dto.response.RoomResponse;
import com.astral.express.pccms.room.dto.response.RoomTypeResponse;
import com.astral.express.pccms.room.entity.Room;
import com.astral.express.pccms.room.entity.RoomType;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface RoomMapper {

    RoomType toRoomType(RoomTypeRequest request);

    void updateRoomType(RoomTypeRequest request, @MappingTarget RoomType roomType);

    @Mapping(target = "roomType", ignore = true)
    Room toRoom(RoomRequest request);

    @Mapping(target = "roomType", ignore = true)
    void updateRoom(RoomRequest request, @MappingTarget Room room);

    RoomTypeResponse toRoomTypeResponse(RoomType roomType);

    @Mapping(target = "roomTypeId", source = "roomType.id")
    @Mapping(target = "roomTypeName", source = "roomType.name")
    RoomResponse toRoomResponse(Room room);
}
