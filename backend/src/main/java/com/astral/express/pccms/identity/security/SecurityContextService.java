package com.astral.express.pccms.identity.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("security")
public class SecurityContextService {

    public UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            return null;
        }
        return (UUID) auth.getPrincipal();
    }

    public boolean isOwner(UUID resourceOwnerId) {
        UUID currentUserId = getCurrentUserId();
        if (currentUserId == null || resourceOwnerId == null) {
            return false;
        }
        return currentUserId.equals(resourceOwnerId);
    }

    public boolean isAdminOrStaff() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getAuthorities() == null) {
            return false;
        }
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") 
                            || a.getAuthority().equals("ROLE_STAFF")
                            || a.getAuthority().equals("ROLE_VETERINARIAN")
                            || a.getAuthority().equals("ROLE_STAFF"));
    }

    public boolean hasAnyRole(String... roleCodes) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getAuthorities() == null) {
            return false;
        }
        return auth.getAuthorities().stream()
                .anyMatch(authority -> {
                    for (String roleCode : roleCodes) {
                        if (authority.getAuthority().equals("ROLE_" + roleCode)) {
                            return true;
                        }
                    }
                    return false;
                });
    }
}
