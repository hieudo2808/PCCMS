package com.astral.express.pccms.user.service;

import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.user.entity.Users;
import org.springframework.stereotype.Component;

@Component
class AccountProtectionPolicy {

    void assertNotProtectedAdmin(Users user) {
        if (isAdmin(user)) {
            throw new BusinessException(ErrorCode.ERR_ACC_009_PROTECTED_ADMIN_ACCOUNT);
        }
    }

    void assertNotAdminRoleRequest(String roleCode) {
        if ("ADMIN".equalsIgnoreCase(UserText.normalize(roleCode))) {
            throw new BusinessException(ErrorCode.ERR_ACC_009_PROTECTED_ADMIN_ACCOUNT);
        }
    }

    private boolean isAdmin(Users user) {
        return user.getRole() != null && "ADMIN".equalsIgnoreCase(user.getRole().getCode());
    }
}
