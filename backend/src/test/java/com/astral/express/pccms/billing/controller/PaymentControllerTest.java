package com.astral.express.pccms.billing.controller;

import com.astral.express.pccms.common.exception.GlobalExceptionHandler;
import com.astral.express.pccms.billing.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private PaymentController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void recordPayment_success() throws Exception {
        mockMvc.perform(post("/v1/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"invoiceId\":\"" + UUID.randomUUID() + "\",\"amountVnd\":100,\"methodCode\":\"CASH\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(201));
    }

    @Test
    void createOwnerPaymentRequest_success() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(post("/v1/payments/me/invoices/{invoiceId}/payment-requests", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amountVnd\":100,\"methodCode\":\"BANK_TRANSFER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(201));
    }

    @Test
    void updatePaymentStatus_success() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(patch("/v1/payments/{paymentId}/status", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"statusCode\":\"SUCCEEDED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
