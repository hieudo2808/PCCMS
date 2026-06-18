package com.astral.express.pccms.boarding.service;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class BoardingPricingPolicyTest {
    private final BoardingPricingPolicy policy = new BoardingPricingPolicy();

    @Test
    void should_roundPartialDayUpToBillableDay() {
        OffsetDateTime startAt = OffsetDateTime.parse("2026-01-01T08:00:00+07:00");
        OffsetDateTime endAt = startAt.plusDays(1).plusMinutes(1);

        assertThat(policy.calculateBillableDays(startAt, endAt)).isEqualTo(2);
        assertThat(policy.calculateAmount(startAt, endAt, 150_000L)).isEqualTo(300_000L);
    }

    @Test
    void should_chargeAtLeastOneDayForZeroOrNegativeRange() {
        OffsetDateTime startAt = OffsetDateTime.parse("2026-01-01T08:00:00+07:00");

        assertThat(policy.calculateBillableDays(startAt, startAt)).isEqualTo(1);
        assertThat(policy.calculateBillableDays(startAt, startAt.minusHours(1))).isEqualTo(1);
    }
}
