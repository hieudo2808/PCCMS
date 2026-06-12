package com.astral.express.pccms.schedule.dto.response;

import java.util.List;

public record WeeklySchedulePlanResponse(
        Integer createdCount,
        Integer skippedCount,
        List<WeeklySchedulePlanItemResponse> items
) {
}
