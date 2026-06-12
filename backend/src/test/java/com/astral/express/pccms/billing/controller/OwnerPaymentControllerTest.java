package com.astral.express.pccms.billing.controller;

import com.astral.express.pccms.billing.dto.request.OwnerPaymentRequest;
import com.astral.express.pccms.billing.dto.response.PaymentResponse;
import com.astral.express.pccms.billing.entity.PaymentMethod;
import com.astral.express.pccms.billing.entity.PaymentStatus;
import com.astral.express.pccms.billing.service.PaymentService;
import com.astral.express.pccms.common.exception.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class OwnerPaymentControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private OwnerPaymentController ownerPaymentController;

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(ownerPaymentController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void should_ReturnCreated_when_CreateOwnerPaymentRequest() throws Exception {
        UUID invoiceId = UUID.randomUUID();
        OwnerPaymentRequest request = new OwnerPaymentRequest(1000L, PaymentMethod.BANK_TRANSFER, "Tx123", "Proof", null);
        PaymentResponse response = new PaymentResponse(UUID.randomUUID(), "PAY1", invoiceId, 1000L, PaymentMethod.BANK_TRANSFER, PaymentStatus.PENDING, null, null, "Tx123");

        given(paymentService.createOwnerPaymentRequest(eq(invoiceId), any(OwnerPaymentRequest.class))).willReturn(response);

        mockMvc.perform(post("/v1/me/invoices/{invoiceId}/payment-requests", invoiceId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void should_ReturnBadRequest_when_CreateOwnerPaymentRequestMissingData() throws Exception {
        UUID invoiceId = UUID.randomUUID();
        OwnerPaymentRequest request = new OwnerPaymentRequest(null, null, null, null, null);

        mockMvc.perform(post("/v1/me/invoices/{invoiceId}/payment-requests", invoiceId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}
