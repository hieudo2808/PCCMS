package com.astral.express.pccms.boarding.service;

import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class BoardingCodeGenerator {
    private static final DateTimeFormatter CODE_TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    public String generate(String prefix) {
        return prefix + "-" + OffsetDateTime.now().format(CODE_TIMESTAMP_FORMAT);
    }
}
