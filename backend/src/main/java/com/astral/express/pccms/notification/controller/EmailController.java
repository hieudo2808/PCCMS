package com.astral.express.pccms.notification.controller;

import com.astral.express.pccms.notification.dto.AccountCreatedEmailRequest;
import com.astral.express.pccms.notification.service.EmailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.astral.express.pccms.common.dto.ApiResponse;

@RequestMapping("/v1/notifications/emails")
@RestController
@RequiredArgsConstructor
public class EmailController {
    private final EmailService emailService;

    @PostMapping("/account-created")
    @PreAuthorize("hasAuthority('ACCOUNT_MANAGE')")
    public ApiResponse<Void> sendAccountCreatedEmail(
            @Valid @RequestBody AccountCreatedEmailRequest request
    ) {
        emailService.sendAccountCreatedEmail(
                request.email(),
                request.temporaryPassword()
        );

        return ApiResponse.success(null);
    }
}
