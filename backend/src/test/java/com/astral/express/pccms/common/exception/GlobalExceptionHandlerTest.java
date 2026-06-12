package com.astral.express.pccms.common.exception;

import org.junit.jupiter.api.Test;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

    @RestController
    private static class DateTimeController {
        @GetMapping("/date-time")
        String getDateTime(
                @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startAt) {
            return startAt.toString();
        }
    }
}
