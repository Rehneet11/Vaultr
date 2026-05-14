package com.example.vaultr.config;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAPIConfig {
    @Value("${app.openapi.server-url:}")
    private String serverUrl;

    @Bean
    public OpenAPI vaultrOpenAPI() {
        OpenAPI openAPI = new OpenAPI()
                .info(new Info()
                        .title("Vaultr API")
                        .description("Distributed P2P payment wallet engineered for correctness under failure.")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("Rehneet Singh")
                                .url("https://github.com/Rehneet11"))
                        .license(new License().name("Apache 2.0").url("http://springdoc.org")));

        if (serverUrl != null && !serverUrl.isBlank()) {
            openAPI.addServersItem(new Server().url(serverUrl));
        }

        return openAPI;
    }
}
