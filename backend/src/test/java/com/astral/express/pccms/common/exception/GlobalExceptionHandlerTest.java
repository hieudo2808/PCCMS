package com.astral.express.pccms.common.exception;

import org.junit.jupiter.api.Test;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.Map;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new DateTimeController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @Test
    void should_return_bad_request_when_query_datetime_has_invalid_format() throws Exception {
        mockMvc.perform(get("/date-time")
                        .param("startAt", "2026-06-06T09:00:00"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("ERR_400_BAD_REQUEST"));
    }

    @Test
    void should_return_field_errors_when_business_validation_fails() throws Exception {
        mockMvc.perform(get("/business-validation"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("ERR_VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors.capacity").value("Capacity must be at least 1"));
    }

    @Test
    void should_return_validation_failed_with_field_errors_when_request_body_is_invalid() throws Exception {
        mockMvc.perform(post("/request-validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("ERR_VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors.name").value("Name is required"));
    }

    @RestController
    private static class DateTimeController {
        @GetMapping("/date-time")
        String getDateTime(
                @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startAt) {
            return startAt.toString();
        }

        @GetMapping("/business-validation")
        String businessValidation() {
            throw new BusinessValidationException(Map.of("capacity", "Capacity must be at least 1"));
        }

        @PostMapping("/request-validation")
        String requestValidation(@Valid @RequestBody ValidationRequest request) {
            return request.name();
        }
    }

    private record ValidationRequest(@NotBlank(message = "Name is required") String name) {
    }
}
