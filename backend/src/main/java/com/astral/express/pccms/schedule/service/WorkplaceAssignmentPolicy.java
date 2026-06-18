package com.astral.express.pccms.schedule.service;

import com.astral.express.pccms.appointment.entity.ExamRoom;
import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.grooming.entity.GroomingStation;
import com.astral.express.pccms.user.entity.Roles;
import com.astral.express.pccms.user.entity.Users;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WorkplaceAssignmentPolicy {
    private static final String VETERINARIAN_ROLE = "VETERINARIAN";
    private static final String STAFF_ROLE = "STAFF";

    public Cursor createCursor(List<ExamRoom> examRooms, List<GroomingStation> stations) {
        return new Cursor(examRooms, stations);
    }

    public void requireValidManualAssignment(Roles role, ExamRoom examRoom, GroomingStation station) {
        if (examRoom != null && station != null) {
            throw new BusinessException(ErrorCode.ERR_VALIDATION_FAILED);
        }
        String roleCode = role != null ? role.getCode() : "";
        if (VETERINARIAN_ROLE.equals(roleCode) && station != null) {
            throw new BusinessException(ErrorCode.ERR_VALIDATION_FAILED);
        }
        if (STAFF_ROLE.equals(roleCode) && examRoom != null) {
            throw new BusinessException(ErrorCode.ERR_VALIDATION_FAILED);
        }
    }

    public static final class Cursor {
        private final List<ExamRoom> examRooms;
        private final List<GroomingStation> stations;
        private int examRoomIndex;
        private int stationIndex;

        private Cursor(List<ExamRoom> examRooms, List<GroomingStation> stations) {
            this.examRooms = examRooms == null ? List.of() : examRooms;
            this.stations = stations == null ? List.of() : stations;
        }

        public WorkplaceAssignment assignFor(Users staff) {
            String roleCode = staff.getRole() != null ? staff.getRole().getCode() : "";
            if (VETERINARIAN_ROLE.equals(roleCode) && !examRooms.isEmpty()) {
                ExamRoom room = examRooms.get(examRoomIndex % examRooms.size());
                examRoomIndex++;
                return new WorkplaceAssignment(room, null);
            }
            if (STAFF_ROLE.equals(roleCode) && !stations.isEmpty()) {
                GroomingStation station = stations.get(stationIndex % stations.size());
                stationIndex++;
                return new WorkplaceAssignment(null, station);
            }
            return new WorkplaceAssignment(null, null);
        }
    }

    public record WorkplaceAssignment(ExamRoom examRoom, GroomingStation station) {
    }
}
