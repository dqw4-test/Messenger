package com.example.socialnetwork.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Social Network API")
                        .description("JWT-secured API for accounts, privacy, attachments, direct chats, groups and search")
                        .version("v1")
                        .contact(new Contact().name("SocialNetwork Team")));
    }
}
