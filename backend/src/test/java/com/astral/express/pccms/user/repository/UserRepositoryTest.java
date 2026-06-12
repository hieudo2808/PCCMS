package com.astral.express.pccms.user.repository;

import com.astral.express.pccms.user.entity.Roles;
import com.astral.express.pccms.user.entity.Users;
import com.astral.express.pccms.user.entity.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

import org.springframework.transaction.annotation.Transactional;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@Transactional
class UserRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");
    }

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    private Roles savedRole;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        roleRepository.deleteAll();

        Roles role = Roles.builder()
                .code("TEST_ROLE")
                .name("Test Role")
                .description("A role for testing")
                .isActive(true)
                .build();
        savedRole = roleRepository.save(role);
    }

    @Test
    void should_PersistAndRetrieveUser_when_MappedCorrectly() {
        // Arrange
        Users user = Users.builder()
                .email("test@pccms.vn")
                .phone("0123456789")
                .passwordHash("hashed_password")
                .fullName("Test User")
                .role(savedRole)
                .statusCode(UserStatus.ACTIVE)
                .build();

        // Act
        Users savedUser = userRepository.saveAndFlush(user);
        Optional<Users> retrievedUserOpt = userRepository.findById(savedUser.getId());

        // Assert
        assertThat(retrievedUserOpt).isPresent();
        Users retrievedUser = retrievedUserOpt.get();
        assertThat(retrievedUser.getEmail()).isEqualTo("test@pccms.vn");
        assertThat(retrievedUser.getPhone()).isEqualTo("0123456789");
        assertThat(retrievedUser.getPasswordHash()).isEqualTo("hashed_password");
        assertThat(retrievedUser.getRole().getCode()).isEqualTo("TEST_ROLE");
        assertThat(retrievedUser.getStatusCode()).isEqualTo(UserStatus.ACTIVE);
        
        // Assert Auditable fields
        assertThat(retrievedUser.getCreatedAt()).isNotNull();
        assertThat(retrievedUser.getUpdatedAt()).isNotNull();
    }
}
