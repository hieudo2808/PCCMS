package com.astral.express.pccms.notification.controller;

import com.astral.express.pccms.notification.dto.AccountCreatedEmailRequest;
import com.astral.express.pccms.notification.service.EmailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/api/notifications/emails")
@RestController
@RequiredArgsConstructor
public class EmailController {
    private final EmailService emailService;

    @PostMapping("/account-created")
    public ResponseEntity<Void> sendAccountCreatedEmail(
            @Valid @RequestBody AccountCreatedEmailRequest request
    ) {
        emailService.sendAccountCreatedEmail(
                request.email(),
                request.temporaryPassword()
        );

        return ResponseEntity.accepted().build();
    }
}