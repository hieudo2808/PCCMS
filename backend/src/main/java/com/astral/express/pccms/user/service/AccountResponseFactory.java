package com.astral.express.pccms.user.service;

import com.astral.express.pccms.user.dto.response.AccountResponse;
import com.astral.express.pccms.user.entity.Roles;
import com.astral.express.pccms.user.entity.Users;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
class AccountResponseFactory {

    AccountResponse toAccountResponse(Users user) {
        Roles role = user.getRole();
        String roleCode = role == null ? null : role.getCode();
        return new AccountResponse(
                user.getId(),
                user.getEmail(),
                user.getPhone(),
                user.getFullName(),
                roleCode,
                role == null ? null : role.getName(),
                roleCode == null ? List.of() : List.of(roleCode),
                user.getStatusCode(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
