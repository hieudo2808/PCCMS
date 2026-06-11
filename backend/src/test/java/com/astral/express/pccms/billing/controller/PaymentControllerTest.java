package com.astral.express.pccms.billing.controller;

import com.astral.express.pccms.billing.dto.request.OwnerPaymentRequest;
import com.astral.express.pccms.billing.dto.request.PaymentStatusUpdateRequest;
import com.astral.express.pccms.billing.dto.request.RecordPaymentRequest;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private PaymentController paymentController;

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(paymentController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void should_ReturnCreated_when_RecordPayment() throws Exception {
        UUID invoiceId = UUID.randomUUID();
        RecordPaymentRequest request = new RecordPaymentRequest(invoiceId, 1000L, PaymentMethod.CASH, null, "Note");
        PaymentResponse response = new PaymentResponse(UUID.randomUUID(), "PAY1", invoiceId, 1000L, PaymentMethod.CASH, PaymentStatus.SUCCEEDED, null, null, "Note");

        given(paymentService.recordPayment(any(RecordPaymentRequest.class))).willReturn(response);

        mockMvc.perform(post("/v1/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void should_ReturnBadRequest_when_RecordPaymentWithMissingData() throws Exception {
        RecordPaymentRequest request = new RecordPaymentRequest(null, null, null, null, null);

        mockMvc.perform(post("/v1/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void should_ReturnCreated_when_CreateOwnerPaymentRequest() throws Exception {
        UUID invoiceId = UUID.randomUUID();
        OwnerPaymentRequest request = new OwnerPaymentRequest(1000L, PaymentMethod.BANK_TRANSFER, "Tx123", "Proof", null);
        PaymentResponse response = new PaymentResponse(UUID.randomUUID(), "PAY2", invoiceId, 1000L, PaymentMethod.BANK_TRANSFER, PaymentStatus.PENDING, null, null, "Tx123");

        given(paymentService.createOwnerPaymentRequest(eq(invoiceId), any(OwnerPaymentRequest.class))).willReturn(response);

        mockMvc.perform(post("/v1/payments/me/invoices/{invoiceId}/payment-requests", invoiceId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void should_ReturnOk_when_UpdatePaymentStatus() throws Exception {
        UUID paymentId = UUID.randomUUID();
        PaymentStatusUpdateRequest request = new PaymentStatusUpdateRequest(PaymentStatus.SUCCEEDED, "OK");
        PaymentResponse response = new PaymentResponse(paymentId, "PAY3", UUID.randomUUID(), 1000L, PaymentMethod.CASH, PaymentStatus.SUCCEEDED, null, null, "OK");

        given(paymentService.updatePaymentStatus(eq(paymentId), eq(PaymentStatus.SUCCEEDED), eq("OK"))).willReturn(response);

        mockMvc.perform(patch("/v1/payments/{paymentId}/status", paymentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void should_ReturnBadRequest_when_UpdatePaymentStatusMissingStatusCode() throws Exception {
        UUID paymentId = UUID.randomUUID();
        String content = "{\"note\":\"OK\"}"; // Missing statusCode

        mockMvc.perform(patch("/v1/payments/{paymentId}/status", paymentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(content))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}
