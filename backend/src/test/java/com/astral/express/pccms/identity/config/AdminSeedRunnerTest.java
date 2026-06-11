package com.astral.express.pccms.identity.config;

import com.astral.express.pccms.user.entity.Roles;
import com.astral.express.pccms.user.entity.UserStatus;
import com.astral.express.pccms.user.entity.Users;
import com.astral.express.pccms.user.repository.RoleRepository;
import com.astral.express.pccms.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class AdminSeedRunnerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    void should_NotSeedAdmin_when_Disabled() throws Exception {
        AdminSeedProperties properties = properties(false, false);
        AdminSeedRunner runner = new AdminSeedRunner(properties, userRepository, roleRepository, passwordEncoder);

        runner.run();

        verifyNoInteractions(userRepository, roleRepository, passwordEncoder);
    }

    @Test
    void should_CreateAdminWithEncodedPassword_when_AdminDoesNotExist() throws Exception {
        AdminSeedProperties properties = properties(true, false);
        Roles adminRole = Roles.builder().code("ADMIN").name("Administrator").isActive(true).build();
        given(userRepository.findByEmail(properties.getEmail())).willReturn(Optional.empty());
        given(roleRepository.findByCode("ADMIN")).willReturn(Optional.of(adminRole));
        given(passwordEncoder.encode(properties.getPassword())).willReturn("peppered-hash");

        AdminSeedRunner runner = new AdminSeedRunner(properties, userRepository, roleRepository, passwordEncoder);
        runner.run();

        ArgumentCaptor<Users> captor = ArgumentCaptor.forClass(Users.class);
        verify(userRepository).save(captor.capture());
        Users saved = captor.getValue();
        assertThat(saved.getEmail()).isEqualTo("admin@pccms.vn");
        assertThat(saved.getFullName()).isEqualTo("System Admin");
        assertThat(saved.getPasswordHash()).isEqualTo("peppered-hash");
        assertThat(saved.getStatusCode()).isEqualTo(UserStatus.ACTIVE);
        assertThat(saved.getRole()).isSameAs(adminRole);
    }

    @Test
    void should_NotResetPassword_when_AdminExistsAndResetDisabled() throws Exception {
        AdminSeedProperties properties = properties(true, false);
        Users existingAdmin = Users.builder()
                .email(properties.getEmail())
                .passwordHash("old-hash")
                .build();
        given(userRepository.findByEmail(properties.getEmail())).willReturn(Optional.of(existingAdmin));

        AdminSeedRunner runner = new AdminSeedRunner(properties, userRepository, roleRepository, passwordEncoder);
        runner.run();

        verify(userRepository, never()).save(existingAdmin);
        verifyNoInteractions(roleRepository, passwordEncoder);
        assertThat(existingAdmin.getPasswordHash()).isEqualTo("old-hash");
    }

    @Test
    void should_ResetPasswordOnly_when_AdminExistsAndResetEnabled() throws Exception {
        AdminSeedProperties properties = properties(true, true);
        Users existingAdmin = Users.builder()
                .email(properties.getEmail())
                .passwordHash("old-hash")
                .build();
        given(userRepository.findByEmail(properties.getEmail())).willReturn(Optional.of(existingAdmin));
        given(passwordEncoder.encode(properties.getPassword())).willReturn("new-peppered-hash");

        AdminSeedRunner runner = new AdminSeedRunner(properties, userRepository, roleRepository, passwordEncoder);
        runner.run();

        verify(userRepository).save(existingAdmin);
        assertThat(existingAdmin.getPasswordHash()).isEqualTo("new-peppered-hash");
    }

    private AdminSeedProperties properties(boolean enabled, boolean resetPassword) {
        AdminSeedProperties properties = new AdminSeedProperties();
        properties.setEnabled(enabled);
        properties.setResetPassword(resetPassword);
        properties.setEmail("admin@pccms.vn");
        properties.setPassword("admin123");
        properties.setFullName("System Admin");
        properties.setRoleCode("ADMIN");
        return properties;
    }
}
