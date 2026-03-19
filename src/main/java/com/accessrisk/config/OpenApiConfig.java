package com.accessrisk.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI accessRiskOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Access Risk Monitoring Platform API")
                        .description("""
                                Enterprise-grade Identity Access Governance (IAG) system.

                                Manage users, roles, and permissions. Define Segregation of Duties (SoD)
                                risk rules and run automated risk analysis to detect conflicting
                                permission combinations across your user base.
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Platform Engineering")
                                .email("platform@company.com"))
                        .license(new License()
                                .name("Internal Use Only")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local development")
                ));
    }
}
