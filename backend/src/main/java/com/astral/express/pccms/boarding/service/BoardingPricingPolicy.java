package com.astral.express.pccms.boarding.service;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.OffsetDateTime;

@Component
public class BoardingPricingPolicy {

    public Long calculateAmount(OffsetDateTime startAt, OffsetDateTime endAt, Long unitPrice) {
        long billableDays = calculateBillableDays(startAt, endAt);
        return billableDays * unitPrice;
    }

    public long calculateBillableDays(OffsetDateTime startAt, OffsetDateTime endAt) {
        long minutes = Duration.between(startAt, endAt).toMinutes();
        if (minutes <= 0) {
            return 1;
        }
        long oneDayMinutes = 24L * 60L;
        return Math.max(1, (minutes + oneDayMinutes - 1) / oneDayMinutes);
    }
}
