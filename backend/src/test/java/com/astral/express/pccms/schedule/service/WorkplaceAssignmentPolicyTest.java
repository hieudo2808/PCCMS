package com.astral.express.pccms.schedule.service;

import com.astral.express.pccms.appointment.entity.ExamRoom;
import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.grooming.entity.GroomingStation;
import com.astral.express.pccms.user.entity.Roles;
import com.astral.express.pccms.user.entity.Users;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WorkplaceAssignmentPolicyTest {
    private final WorkplaceAssignmentPolicy policy = new WorkplaceAssignmentPolicy();

    @Test
    void shouldAssignExamRoomsRoundRobinForVeterinarians() {
        ExamRoom first = examRoom("R1");
        ExamRoom second = examRoom("R2");
        WorkplaceAssignmentPolicy.Cursor cursor = policy.createCursor(List.of(first, second), List.of());

        assertThat(cursor.assignFor(staffWithRole("VETERINARIAN")).examRoom()).isEqualTo(first);
        assertThat(cursor.assignFor(staffWithRole("VETERINARIAN")).examRoom()).isEqualTo(second);
        assertThat(cursor.assignFor(staffWithRole("VETERINARIAN")).examRoom()).isEqualTo(first);
    }

    @Test
    void shouldAssignStationsRoundRobinForStaff() {
        GroomingStation first = station("S1");
        GroomingStation second = station("S2");
        WorkplaceAssignmentPolicy.Cursor cursor = policy.createCursor(List.of(), List.of(first, second));

        assertThat(cursor.assignFor(staffWithRole("STAFF")).station()).isEqualTo(first);
        assertThat(cursor.assignFor(staffWithRole("STAFF")).station()).isEqualTo(second);
        assertThat(cursor.assignFor(staffWithRole("STAFF")).station()).isEqualTo(first);
    }

    @Test
    void shouldNotAssignWorkplaceForOtherRoles() {
        WorkplaceAssignmentPolicy.Cursor cursor = policy.createCursor(List.of(examRoom("R1")), List.of(station("S1")));

        WorkplaceAssignmentPolicy.WorkplaceAssignment assignment = cursor.assignFor(staffWithRole("RECEPTIONIST"));

        assertThat(assignment.examRoom()).isNull();
        assertThat(assignment.station()).isNull();
    }

    @Test
    void shouldRejectManualAssignmentThatConflictsWithRole() {
        assertThatThrownBy(() -> policy.requireValidManualAssignment(
                role("VETERINARIAN"),
                null,
                station("S1")))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> policy.requireValidManualAssignment(
                role("STAFF"),
                examRoom("R1"),
                null))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void shouldRejectTwoWorkplacesForOneSchedule() {
        assertThatThrownBy(() -> policy.requireValidManualAssignment(
                role("RECEPTIONIST"),
                examRoom("R1"),
                station("S1")))
                .isInstanceOf(BusinessException.class);
    }

    private static Users staffWithRole(String roleCode) {
        Users staff = new Users();
        staff.setId(UUID.randomUUID());
        staff.setRole(role(roleCode));
        return staff;
    }

    private static Roles role(String roleCode) {
        Roles role = new Roles();
        role.setId(UUID.randomUUID());
        role.setCode(roleCode);
        return role;
    }

    private static ExamRoom examRoom(String code) {
        ExamRoom room = new ExamRoom();
        room.setId(UUID.randomUUID());
        room.setRoomCode(code);
        return room;
    }

    private static GroomingStation station(String code) {
        GroomingStation station = new GroomingStation();
        station.setId(UUID.randomUUID());
        station.setStationCode(code);
        return station;
    }
}
