package com.astral.express.pccms.identity.service;

import com.astral.express.pccms.user.entity.Roles;
import com.astral.express.pccms.user.entity.Permission;
import com.astral.express.pccms.user.entity.Users;
import com.astral.express.pccms.user.entity.UserStatus;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CustomUserDetailsTest {

    @Test
    void should_InitializeCorrectly() {
        Users user = new Users();
        user.setId(UUID.randomUUID());
        user.setEmail("test@test.com");
        user.setPasswordHash("hash");
        user.setStatusCode(UserStatus.ACTIVE);

        Roles role = new Roles();
        role.setCode("ADMIN");

        Permission perm1 = new Permission();
        perm1.setCode("READ_ALL");
        Permission perm2 = new Permission();
        perm2.setCode("WRITE_ALL");

        role.setPermissions(Set.of(perm1, perm2));
        user.setRole(role);

        CustomUserDetails details = new CustomUserDetails(user);

        assertThat(details.getId()).isEqualTo(user.getId());
        assertThat(details.getUsername()).isEqualTo("test@test.com");
        assertThat(details.getPassword()).isEqualTo("hash");
        assertThat(details.isAccountNonLocked()).isTrue();
        assertThat(details.isEnabled()).isTrue();
        assertThat(details.getAuthorities()).hasSize(3);
        
        assertThat(details.getAuthorities().stream().map(GrantedAuthority::getAuthority))
            .containsExactlyInAnyOrder("READ_ALL", "WRITE_ALL", "ROLE_ADMIN");
    }

    @Test
    void should_BeDisabled_when_UserIsNotActive() {
        Users user = new Users();
        user.setStatusCode(UserStatus.LOCKED);
        
        Roles role = new Roles();
        role.setCode("USER");
        role.setPermissions(Set.of());
        user.setRole(role);

        CustomUserDetails details = new CustomUserDetails(user);

        assertThat(details.isEnabled()).isFalse();
    }
}
