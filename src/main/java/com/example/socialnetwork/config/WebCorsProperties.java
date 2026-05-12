package com.example.socialnetwork.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.web")
public record WebCorsProperties(List<String> allowedOrigins) {
}
