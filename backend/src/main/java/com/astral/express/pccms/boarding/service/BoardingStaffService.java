package com.astral.express.pccms.boarding.service;

import com.astral.express.pccms.boarding.dto.request.UpsertCareLogRequest;
import com.astral.express.pccms.boarding.dto.response.CareLogResponse;
import com.astral.express.pccms.boarding.dto.response.StaffBoardingStayResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface BoardingStaffService {

    List<StaffBoardingStayResponse> listActiveStays();

    List<CareLogResponse> listSessionLogs(UUID sessionId, LocalDate logDate);

    CareLogResponse upsertCareLog(UUID staffId, UpsertCareLogRequest request);
}
