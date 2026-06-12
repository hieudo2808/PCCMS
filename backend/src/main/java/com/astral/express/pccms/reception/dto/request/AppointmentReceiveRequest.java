package com.astral.express.pccms.reception.dto.request;

import java.util.UUID;

public record AppointmentReceiveRequest(UUID doctorId, String note) {}
