package com.astral.express.pccms.identity.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "pccms.seed.admin")
public class AdminSeedProperties {
    private boolean enabled = false;
    private boolean resetPassword = false;
    private String email;
    private String password;
    private String fullName = "System Admin";
    private String roleCode = "ADMIN";
}
