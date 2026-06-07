package com.astral.express.pccms.user.mapper;

import com.astral.express.pccms.user.dto.request.AdminUpdateUserRequest;
import com.astral.express.pccms.user.dto.request.CreateUserRequest;
import com.astral.express.pccms.user.dto.request.UserProfileUpdateRequest;
import com.astral.express.pccms.user.dto.response.UserResponse;
import com.astral.express.pccms.user.entity.Users;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(source = "role.code", target = "roleCode")
    UserResponse toUserResponse(Users user);

    @Mapping(source = "roleCode", target = "role.code")
    Users toUser(CreateUserRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(source = "roleCode", target = "role.code")
    void updateFromAdmin(AdminUpdateUserRequest request, @MappingTarget Users user);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateProfile(UserProfileUpdateRequest request, @MappingTarget Users user);
}
