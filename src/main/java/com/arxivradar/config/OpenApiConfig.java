package com.arxivradar.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI arxivRadarOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ArxivRadar API")
                        .description("AI 论文情报站后端接口文档")
                        .version("v1")
                        .license(new License().name("MIT")));
    }
}
