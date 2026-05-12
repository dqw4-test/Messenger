package com.example.socialnetwork.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {
    private String issuer;
    private String accessSecret;
    private String refreshSecret;
    private long accessTtlMinutes;
    private long refreshTtlDays;
}
