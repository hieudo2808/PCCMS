package com.astral.express.pccms.user.dto.response;

public record AccountCredentialResponse(
        AccountResponse account,
        String temporaryPassword,
        Boolean emailSent
) {
}
