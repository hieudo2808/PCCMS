package com.astral.express.pccms.notification.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "pccms.mail")
public class MailProperties {
    private String from;
    private String appName = "Pawluna";
    private String teamName = "Astral Team";
}