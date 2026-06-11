package com.astral.express.pccms.billing.controller;

import com.astral.express.pccms.billing.dto.response.InvoiceResponse;
import com.astral.express.pccms.billing.service.InvoiceService;
import com.astral.express.pccms.common.dto.PageResponse;
import com.astral.express.pccms.common.exception.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class InvoiceControllerTest {

    private MockMvc mockMvc;

    @Mock
    private InvoiceService invoiceService;

    @InjectMocks
    private InvoiceController invoiceController;

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(invoiceController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void should_ReturnPageResponse_when_ListMyInvoices() throws Exception {
        InvoiceResponse response = new InvoiceResponse(UUID.randomUUID(), "INV1", null, null, null, null, null, null, null, null);
        PageResponse<InvoiceResponse> pageResponse = PageResponse.of(new PageImpl<>(List.of(response)));

        given(invoiceService.listMyInvoices(any(Pageable.class))).willReturn(pageResponse);

        mockMvc.perform(get("/v1/invoices/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void should_ReturnPageResponse_when_ListInvoices() throws Exception {
        InvoiceResponse response = new InvoiceResponse(UUID.randomUUID(), "INV1", null, null, null, null, null, null, null, null);
        PageResponse<InvoiceResponse> pageResponse = PageResponse.of(new PageImpl<>(List.of(response)));

        given(invoiceService.listInvoices(any(Pageable.class))).willReturn(pageResponse);

        mockMvc.perform(get("/v1/invoices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void should_ReturnInvoiceResponse_when_GetInvoice() throws Exception {
        UUID invoiceId = UUID.randomUUID();
        InvoiceResponse response = new InvoiceResponse(invoiceId, "INV1", null, null, null, null, null, null, null, null);

        given(invoiceService.getInvoice(invoiceId)).willReturn(response);

        mockMvc.perform(get("/v1/invoices/{invoiceId}", invoiceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.invoiceCode").value("INV1"));
    }
}
