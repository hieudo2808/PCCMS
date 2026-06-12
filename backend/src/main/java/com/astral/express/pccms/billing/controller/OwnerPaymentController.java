package com.astral.express.pccms.billing.controller;

import com.astral.express.pccms.billing.dto.request.OwnerPaymentRequest;
import com.astral.express.pccms.billing.dto.response.PaymentResponse;
import com.astral.express.pccms.billing.service.PaymentService;
import com.astral.express.pccms.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/v1/me/invoices")
@RequiredArgsConstructor
public class OwnerPaymentController {
    private final PaymentService paymentService;

    @PostMapping("/{invoiceId}/payment-requests")
    @PreAuthorize("hasAuthority('INVOICE_READ')")
    public ApiResponse<PaymentResponse> createOwnerPaymentRequest(
            @PathVariable UUID invoiceId,
            @Valid @RequestBody OwnerPaymentRequest request) {
        return ApiResponse.created(paymentService.createOwnerPaymentRequest(invoiceId, request));
    }
}
