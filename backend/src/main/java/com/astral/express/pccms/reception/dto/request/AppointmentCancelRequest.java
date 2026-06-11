package com.astral.express.pccms.reception.dto.request;

import jakarta.validation.constraints.Size;

public record AppointmentCancelRequest(@Size(max = 500) String reason) {}
