package com.astral.express.pccms.identity.service;

import com.astral.express.pccms.user.entity.Users;
import com.astral.express.pccms.user.repository.UserRepository;
import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void should_ReturnUserDetails_when_LoadUserByUsername_UserExists() {
        // GIVEN
        String email = "test@example.com";
        Users mockUser = new Users();
        mockUser.setId(UUID.randomUUID());
        mockUser.setEmail(email);
        mockUser.setPasswordHash("hashedPassword");
        
        com.astral.express.pccms.user.entity.Roles role = new com.astral.express.pccms.user.entity.Roles();
        role.setCode("USER");
        role.setPermissions(java.util.Set.of());
        mockUser.setRole(role);
        
        when(userRepository.findByEmailWithRoleAndPermissions(email)).thenReturn(Optional.of(mockUser));

        // WHEN
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);

        // THEN
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo(email);
        verify(userRepository).findByEmailWithRoleAndPermissions(email);
    }

    @Test
    void should_ThrowException_when_LoadUserByUsername_UserNotFound() {
        // GIVEN
        String email = "notfound@example.com";
        when(userRepository.findByEmailWithRoleAndPermissions(email)).thenReturn(Optional.empty());

        // WHEN & THEN
        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername(email))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_ACC_002_USER_NOT_FOUND);
    }

    @Test
    void should_ReturnUserDetails_when_LoadUserById_UserExists() {
        // GIVEN
        UUID userId = UUID.randomUUID();
        Users mockUser = new Users();
        mockUser.setId(userId);
        mockUser.setEmail("test@example.com");
        mockUser.setPasswordHash("hashedPassword");
        
        com.astral.express.pccms.user.entity.Roles role = new com.astral.express.pccms.user.entity.Roles();
        role.setCode("USER");
        role.setPermissions(java.util.Set.of());
        mockUser.setRole(role);
        
        when(userRepository.findByIdWithRoleAndPermissions(userId)).thenReturn(Optional.of(mockUser));

        // WHEN
        UserDetails userDetails = customUserDetailsService.loadUserById(userId);

        // THEN
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo("test@example.com");
        verify(userRepository).findByIdWithRoleAndPermissions(userId);
    }

    @Test
    void should_ThrowException_when_LoadUserById_UserNotFound() {
        // GIVEN
        UUID userId = UUID.randomUUID();
        when(userRepository.findByIdWithRoleAndPermissions(userId)).thenReturn(Optional.empty());

        // WHEN & THEN
        assertThatThrownBy(() -> customUserDetailsService.loadUserById(userId))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_ACC_002_USER_NOT_FOUND);
    }
}
