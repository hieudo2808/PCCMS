package com.astral.express.pccms.notification.service;

import com.astral.express.pccms.notification.config.MailProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailTemplateService {
    private final MailProperties mailProperties;

    public String buildAccountCreatedSubject() {
        return "Tài khoản của bạn đã được tạo - " + mailProperties.getAppName();
    }

    public String buildAccountCreatedContent(String email, String temporaryPassword) {
        return """
                Xin chào,

                Tài khoản của bạn trên hệ thống %s đã được tạo thành công.

                Thông tin đăng nhập:
                - Email: %s
                - Mật khẩu tạm thời: %s

                Vui lòng đăng nhập và đổi mật khẩu sau lần đăng nhập đầu tiên.

                Trân trọng,
                %s
                """.formatted(
                mailProperties.getAppName(),
                email,
                temporaryPassword,
                mailProperties.getTeamName()
        );
    }
}