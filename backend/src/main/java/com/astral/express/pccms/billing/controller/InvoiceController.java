package com.astral.express.pccms.billing.controller;

import com.astral.express.pccms.billing.dto.response.InvoiceResponse;
import com.astral.express.pccms.billing.service.InvoiceService;
import com.astral.express.pccms.common.dto.ApiResponse;
import com.astral.express.pccms.common.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/v1/invoices")
@RequiredArgsConstructor
public class InvoiceController {
    private final InvoiceService invoiceService;

    @GetMapping("/my")
    @PreAuthorize("hasAuthority('INVOICE_READ')")
    public ApiResponse<PageResponse<InvoiceResponse>> listMyInvoices(
            @PageableDefault(size = 20, sort = "issuedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success(invoiceService.listMyInvoices(pageable));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('INVOICE_MANAGE')")
    public ApiResponse<PageResponse<InvoiceResponse>> listInvoices(
            @PageableDefault(size = 20, sort = "issuedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success(invoiceService.listInvoices(pageable));
    }

    @GetMapping("/{invoiceId}")
    @PreAuthorize("hasAnyAuthority('INVOICE_READ', 'INVOICE_MANAGE')")
    public ApiResponse<InvoiceResponse> getInvoice(@PathVariable UUID invoiceId) {
        return ApiResponse.success(invoiceService.getInvoice(invoiceId));
    }
}
