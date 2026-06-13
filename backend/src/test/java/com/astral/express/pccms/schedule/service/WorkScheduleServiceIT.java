package com.astral.express.pccms.schedule.service;

import com.astral.express.pccms.common.AbstractIntegrationTest;
import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.schedule.dto.request.WorkScheduleRequest;
import com.astral.express.pccms.schedule.dto.response.WorkScheduleResponse;
import com.astral.express.pccms.schedule.entity.ScheduleStatus;
import com.astral.express.pccms.schedule.entity.Shift;
import com.astral.express.pccms.schedule.entity.WorkSchedule;
import com.astral.express.pccms.schedule.repository.ShiftRepository;
import com.astral.express.pccms.schedule.repository.WorkScheduleRepository;
import com.astral.express.pccms.user.entity.Roles;
import com.astral.express.pccms.user.entity.UserStatus;
import com.astral.express.pccms.user.entity.Users;
import com.astral.express.pccms.user.repository.RoleRepository;
import com.astral.express.pccms.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WorkScheduleServiceIT extends AbstractIntegrationTest {

    @Autowired
    private WorkScheduleService workScheduleService;

    @Autowired
    private WorkScheduleRepository workScheduleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private ShiftRepository shiftRepository;

    private Users testStaff;
    private Roles testRole;
    private Shift testShift;

    @BeforeEach
    void setUp() {
        workScheduleRepository.deleteAll();
        shiftRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();

        testRole = Roles.builder()
                .code("VETERINARIAN")
                .name("Veterinarian")
                .isActive(true)
                .build();
        testRole = roleRepository.saveAndFlush(testRole);

        testStaff = Users.builder()
                .email("vet@pccms.vn")
                .passwordHash("hash")
                .fullName("Test Vet")
                .role(testRole)
                .statusCode(UserStatus.ACTIVE)
                .build();
        testStaff = userRepository.saveAndFlush(testStaff);

        testShift = new Shift();
        testShift.setCode("MORNING");
        testShift.setName("Sáng");
        testShift.setStartTime(LocalTime.of(8, 0));
        testShift.setEndTime(LocalTime.of(12, 0));
        testShift.setIsActive(true);
        testShift = shiftRepository.saveAndFlush(testShift);
    }

    @Test
    void should_create_schedule_successfully() {
        // Arrange
        LocalDate workDate = LocalDate.now().plusDays(1);
        WorkScheduleRequest request = new WorkScheduleRequest(
                testStaff.getId(), workDate, testShift.getId(), null, null, testRole.getId(), 5, ScheduleStatus.ASSIGNED, "Test schedule"
        );

        // Act
        WorkScheduleResponse response = workScheduleService.createSchedule(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.staffId()).isEqualTo(testStaff.getId());
        
        WorkSchedule saved = workScheduleRepository.findById(response.id()).orElseThrow();
        assertThat(saved.getCapacity()).isEqualTo(5);
        assertThat(saved.getWorkDate()).isEqualTo(workDate);
    }

    @Test
    void should_throw_business_exception_on_duplicate_schedule_creation() {
        // Arrange
        LocalDate workDate = LocalDate.now().plusDays(1);
        WorkScheduleRequest request = new WorkScheduleRequest(
                testStaff.getId(), workDate, testShift.getId(), null, null, testRole.getId(), 5, ScheduleStatus.ASSIGNED, "Test schedule"
        );
        workScheduleService.createSchedule(request); // Save first one

        // Act & Assert
        assertThatThrownBy(() -> workScheduleService.createSchedule(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_VALIDATION_FAILED);
    }

    @Test
    void should_throw_DataIntegrityViolationException_on_concurrent_duplicate_insert() {
        // Arrange
        LocalDate workDate = LocalDate.now().plusDays(1);
        WorkSchedule schedule1 = new WorkSchedule();
        schedule1.setStaff(testStaff);
        schedule1.setWorkDate(workDate);
        schedule1.setShift(testShift);
        schedule1.setRole(testRole);
        schedule1.setCapacity(5);
        schedule1.setStatusCode(ScheduleStatus.ASSIGNED);
        workScheduleRepository.saveAndFlush(schedule1);

        WorkSchedule schedule2 = new WorkSchedule();
        schedule2.setStaff(testStaff);
        schedule2.setWorkDate(workDate); // Same date
        schedule2.setShift(testShift); // Same shift
        schedule2.setRole(testRole);
        schedule2.setCapacity(5);
        schedule2.setStatusCode(ScheduleStatus.ASSIGNED);

        // Act & Assert
        assertThatThrownBy(() -> workScheduleRepository.saveAndFlush(schedule2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
