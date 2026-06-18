package com.astral.express.pccms.billing.service;

import com.astral.express.pccms.billing.entity.PaymentStatus;
import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.EnumSet;

@Component
public class PaymentTransitionPolicy {
    private static final EnumSet<PaymentStatus> PENDING_TARGETS = EnumSet.of(
            PaymentStatus.SUCCEEDED,
            PaymentStatus.FAILED,
            PaymentStatus.CANCELLED);

    public void requireAllowed(PaymentStatus currentStatus, PaymentStatus targetStatus) {
        if (currentStatus == null || targetStatus == null) {
            throw new BusinessException(ErrorCode.ERR_VALIDATION_FAILED);
        }
        if (currentStatus == targetStatus) {
            return;
        }
        if (currentStatus != PaymentStatus.PENDING || !PENDING_TARGETS.contains(targetStatus)) {
            throw new BusinessException(ErrorCode.ERR_VALIDATION_FAILED);
        }
    }
}
