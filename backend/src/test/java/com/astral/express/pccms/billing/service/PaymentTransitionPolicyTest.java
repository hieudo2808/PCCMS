package com.astral.express.pccms.billing.service;

import com.astral.express.pccms.billing.entity.PaymentStatus;
import com.astral.express.pccms.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentTransitionPolicyTest {
    private final PaymentTransitionPolicy policy = new PaymentTransitionPolicy();

    @Test
    void should_AllowPendingToReachSupportedTerminalStatuses() {
        assertThatCode(() -> policy.requireAllowed(PaymentStatus.PENDING, PaymentStatus.SUCCEEDED))
                .doesNotThrowAnyException();
        assertThatCode(() -> policy.requireAllowed(PaymentStatus.PENDING, PaymentStatus.FAILED))
                .doesNotThrowAnyException();
        assertThatCode(() -> policy.requireAllowed(PaymentStatus.PENDING, PaymentStatus.CANCELLED))
                .doesNotThrowAnyException();
    }

    @Test
    void should_AllowIdempotentStatusUpdate() {
        assertThatCode(() -> policy.requireAllowed(PaymentStatus.SUCCEEDED, PaymentStatus.SUCCEEDED))
                .doesNotThrowAnyException();
    }

    @Test
    void should_RejectTransitionFromTerminalStatus() {
        assertThatThrownBy(() -> policy.requireAllowed(PaymentStatus.FAILED, PaymentStatus.SUCCEEDED))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> policy.requireAllowed(PaymentStatus.SUCCEEDED, PaymentStatus.REFUNDED))
                .isInstanceOf(BusinessException.class);
    }
}
