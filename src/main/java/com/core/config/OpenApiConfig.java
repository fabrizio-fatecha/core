package com.core.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.responses.ApiResponse;
import java.time.Duration;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class OpenApiConfig {

    private static final String FIRST_PATH = "/api/v1/products/{rowNumber}";

    private static final String DESCRIPTION = """
            Read-only REST API for a product catalog with **50,000,000 records**.

            Every record is identified by a plain numeric **row number**, from 1 to 50,000,000. \
            Fetch any record directly by its number, or walk through the catalog in order \
            by passing the number to start from.

            Created by **Fabrizio Fatecha**.

            ### Rate limit

            Each IP address can make **%d requests** to the API. After the last one, that IP is blocked \
            for **%d minutes**: every request during that time returns HTTP 429 with a `Retry-After` header \
            (in seconds). Every response shows the requests left in the `X-RateLimit-Remaining` header. \
            Loading this documentation does not count.

            ### Tech stack

            - **Java 21** with virtual threads and records
            - **Spring Boot 4** and **Spring Security 7**, stateless and deny-by-default
            - **PostgreSQL 17** with a numeric primary key and keyset pagination (no `OFFSET`, no `COUNT(*)`)
            - **nginx** as reverse proxy, with per-IP rate limiting
            - **Docker** and **Docker Compose** on **AWS EC2** (Ubuntu 24.04, ARM64, Sao Paulo)
            - **OpenAPI 3** documentation served with springdoc
            """;

    private final String description;
    private final String rateLimitResponse;

    OpenApiConfig(@Value("${app.quota.requests}") int requests, @Value("${app.quota.block}") Duration block) {
        this.description = DESCRIPTION.formatted(requests, block.toMinutes());
        this.rateLimitResponse = "Request limit reached: this IP is blocked for " + block.toMinutes()
                + " minutes. The `Retry-After` header says how many seconds are left.";
    }

    @Bean
    OpenAPI openApi() {
        return new OpenAPI().info(new Info()
                .title("Core API")
                .version("v1")
                .description(description)
                .contact(new Contact().name("Fabrizio Fatecha")));
    }

    @Bean
    OpenApiCustomizer rowNumberLookupFirst() {
        return openApi -> {
            Paths current = openApi.getPaths();
            PathItem first = current.get(FIRST_PATH);
            if (first == null) {
                return;
            }
            Paths ordered = new Paths();
            ordered.addPathItem(FIRST_PATH, first);
            current.forEach((path, item) -> {
                if (!path.equals(FIRST_PATH)) {
                    ordered.addPathItem(path, item);
                }
            });
            openApi.setPaths(ordered);
        };
    }

    @Bean
    OpenApiCustomizer rateLimitResponseOnEveryOperation() {
        return openApi -> openApi.getPaths().values().forEach(item -> item.readOperations().forEach(operation ->
                operation.getResponses().addApiResponse("429", new ApiResponse().description(rateLimitResponse))));
    }
}
