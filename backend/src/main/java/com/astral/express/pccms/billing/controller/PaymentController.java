package com.astral.express.pccms.billing.controller;

import com.astral.express.pccms.billing.dto.request.OwnerPaymentRequest;
import com.astral.express.pccms.billing.dto.request.PaymentStatusUpdateRequest;
import com.astral.express.pccms.billing.dto.request.RecordPaymentRequest;
import com.astral.express.pccms.billing.dto.response.PaymentResponse;
import com.astral.express.pccms.billing.service.PaymentService;
import com.astral.express.pccms.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/v1/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;

    @PostMapping
    @PreAuthorize("hasAuthority('INVOICE_MANAGE')")
    public ApiResponse<PaymentResponse> recordPayment(@Valid @RequestBody RecordPaymentRequest request) {
        return ApiResponse.created(paymentService.recordPayment(request));
    }

    @PostMapping("/me/invoices/{invoiceId}/payment-requests")
    @PreAuthorize("hasAuthority('INVOICE_READ')")
    public ApiResponse<PaymentResponse> createOwnerPaymentRequest(
            @PathVariable UUID invoiceId,
            @Valid @RequestBody OwnerPaymentRequest request) {
        return ApiResponse.created(paymentService.createOwnerPaymentRequest(invoiceId, request));
    }

    @PatchMapping("/{paymentId}/status")
    @PreAuthorize("hasAuthority('INVOICE_MANAGE')")
    public ApiResponse<PaymentResponse> updatePaymentStatus(
            @PathVariable UUID paymentId,
            @Valid @RequestBody PaymentStatusUpdateRequest request) {
        return ApiResponse.success(paymentService.updatePaymentStatus(paymentId, request.statusCode(), request.note()));
    }
}
