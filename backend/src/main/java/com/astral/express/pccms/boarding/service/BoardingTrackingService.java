package com.astral.express.pccms.boarding.service;

import com.astral.express.pccms.boarding.dto.response.BoardingStayResponse;
import com.astral.express.pccms.boarding.dto.response.CareLogResponse;

import java.util.List;
import java.util.UUID;

public interface BoardingTrackingService {

    List<BoardingStayResponse> listActiveStays(UUID ownerId);

    List<CareLogResponse> listCareLogs(UUID ownerId, UUID petId);
}
