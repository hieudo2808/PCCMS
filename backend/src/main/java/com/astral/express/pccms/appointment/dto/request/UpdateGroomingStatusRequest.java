package com.astral.express.pccms.appointment.dto.request;

import com.astral.express.pccms.appointment.entity.GroomingStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateGroomingStatusRequest(@NotNull GroomingStatus status) {}
