package com.via.shinvia.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Shinvia API Documentation")
                        .description("신한 비아(Shinvia) 계좌 및 카드 연동 API 명세서")
                        .version("v1.0.0"));
    }
}
