package com.astral.express.pccms.identity.config;

import com.astral.express.pccms.user.entity.Roles;
import com.astral.express.pccms.user.entity.UserStatus;
import com.astral.express.pccms.user.entity.Users;
import com.astral.express.pccms.user.repository.RoleRepository;
import com.astral.express.pccms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminSeedRunner implements ApplicationRunner {

    private final AdminSeedProperties properties;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        run();
    }

    @Transactional
    void run() {
        if (!properties.isEnabled()) {
            return;
        }
        if (isBlank(properties.getEmail()) || isBlank(properties.getPassword())) {
            log.warn("Admin seed is enabled but email or password is blank; skipping admin seed");
            return;
        }

        userRepository.findByEmail(properties.getEmail())
                .ifPresentOrElse(this::updateExistingAdminIfAllowed, this::createAdminIfRoleExists);
    }

    private void createAdminIfRoleExists() {
        roleRepository.findByCode(properties.getRoleCode())
                .ifPresentOrElse(role -> {
                    Users admin = Users.builder()
                            .email(properties.getEmail())
                            .passwordHash(passwordEncoder.encode(properties.getPassword()))
                            .fullName(properties.getFullName())
                            .role(role)
                            .statusCode(UserStatus.ACTIVE)
                            .build();
                    userRepository.save(admin);
                    log.info("Admin seed created account for {}", properties.getEmail());
                }, () -> log.warn("Admin seed role {} not found; skipping admin seed", properties.getRoleCode()));
    }

    private void updateExistingAdminIfAllowed(Users admin) {
        if (!properties.isResetPassword()) {
            log.info("Admin seed found existing account for {}; password reset disabled", properties.getEmail());
            return;
        }

        admin.setPasswordHash(passwordEncoder.encode(properties.getPassword()));
        userRepository.save(admin);
        log.info("Admin seed reset password for {}", properties.getEmail());
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
