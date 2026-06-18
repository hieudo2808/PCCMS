package com.astral.express.pccms.appointment.service;

import com.astral.express.pccms.appointment.dto.request.CreateBoardingBookingRequest;
import com.astral.express.pccms.appointment.dto.response.BoardingBookingResponse;
import com.astral.express.pccms.appointment.dto.response.RoomTypeOptionResponse;
import com.astral.express.pccms.boarding.entity.BoardingBooking;
import com.astral.express.pccms.boarding.dto.request.BoardingBookingCreateRequest;
import com.astral.express.pccms.boarding.repository.BoardingBookingRepository;
import com.astral.express.pccms.boarding.service.BoardingService;
import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.room.repository.RoomTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AppointmentBoardingBookingUseCase {

    private final BoardingService boardingService;
    private final BoardingBookingRepository boardingBookingRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final AppointmentResponseAssembler assembler;

    @Transactional
    public BoardingBookingResponse createBoardingBooking(CreateBoardingBookingRequest request, UUID ownerId) {
        var createRequest = new BoardingBookingCreateRequest(
                request.petId(),
                request.roomTypeId(),
                ClinicDateTime.toOffsetDateTime(request.checkinDate(), LocalTime.of(14, 0)),
                ClinicDateTime.toOffsetDateTime(request.checkoutDate(), LocalTime.of(11, 0)),
                request.specialCareRequest()
        );
        var dedicatedResponse = boardingService.createBooking(createRequest);
        BoardingBooking booking = boardingBookingRepository.findById(dedicatedResponse.id())
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_APT_001_NOT_FOUND));
        return assembler.toBoardingResponse(booking);
    }

    @Transactional(readOnly = true)
    public List<BoardingBookingResponse> listOwnerBoardingBookings(UUID ownerId) {
        return boardingBookingRepository.findByOwnerId(ownerId).stream()
                .map(assembler::toBoardingResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RoomTypeOptionResponse> listActiveRoomTypes() {
        return roomTypeRepository.findByIsActiveTrueOrderByNameAsc().stream()
                .map(assembler::toRoomTypeOptionResponse)
                .toList();
    }
}
