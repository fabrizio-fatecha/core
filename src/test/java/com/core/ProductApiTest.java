package com.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

@SpringBootTest(properties = {"DB_PASSWORD=unused", "app.quota.requests=100000"})
@AutoConfigureMockMvc
@Testcontainers
class ProductApiTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine")
            .withCopyFileToContainer(MountableFile.forHostPath("db/init/01-schema.sql"),
                    "/docker-entrypoint-initdb.d/01-schema.sql")
            .withCopyFileToContainer(MountableFile.forHostPath("db/init/03-seed.sql"),
                    "/docker-entrypoint-initdb.d/03-seed.sql");

    @Autowired
    MockMvc mvc;

    @Test
    void getByRowNumberReturnsTheMatchingProduct() throws Exception {
        mvc.perform(get("/api/v1/products/{rowNumber}", 77))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rowNumber").value(77))
                .andExpect(jsonPath("$.sku").value("SKU-00000077"))
                .andExpect(jsonPath("$.name").value("Product 77"))
                .andExpect(jsonPath("$.inStock").exists())
                .andExpect(jsonPath("$.cost").doesNotExist())
                .andExpect(jsonPath("$.supplierId").doesNotExist())
                .andExpect(jsonPath("$.stockQty").doesNotExist());
    }

    @Test
    void getFirstAndLastRowNumbersOfTheSeed() throws Exception {
        mvc.perform(get("/api/v1/products/{rowNumber}", 1)).andExpect(status().isOk());
        mvc.perform(get("/api/v1/products/{rowNumber}", 100000)).andExpect(status().isOk());
    }

    @Test
    void getByUnknownRowNumberIsNotFound() throws Exception {
        mvc.perform(get("/api/v1/products/{rowNumber}", 99999999)).andExpect(status().isNotFound());
    }

    @Test
    void getByInvalidRowNumberIsBadRequest() throws Exception {
        mvc.perform(get("/api/v1/products/{rowNumber}", 0)).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/products/{rowNumber}", -5)).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/products/abc")).andExpect(status().isBadRequest());
    }

    @Test
    void listStartsAtRowNumberOneByDefault() throws Exception {
        mvc.perform(get("/api/v1/products").param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(5)))
                .andExpect(jsonPath("$.items[0].rowNumber").value(1))
                .andExpect(jsonPath("$.items[4].rowNumber").value(5))
                .andExpect(jsonPath("$.nextRowNumber").value(6));
    }

    @Test
    void listContinuesFromTheGivenRowNumber() throws Exception {
        mvc.perform(get("/api/v1/products").param("from", "6").param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].rowNumber").value(6))
                .andExpect(jsonPath("$.items[4].rowNumber").value(10))
                .andExpect(jsonPath("$.nextRowNumber").value(11));
    }

    @Test
    void listEndsWithoutNextRowNumber() throws Exception {
        mvc.perform(get("/api/v1/products").param("from", "99999").param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[1].rowNumber").value(100000))
                .andExpect(jsonPath("$.nextRowNumber").value(nullValue()));
    }

    @Test
    void listDefaultsToTwentyFiveItems() throws Exception {
        mvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(25)));
    }

    @Test
    void pageSizeIsCapped() throws Exception {
        mvc.perform(get("/api/v1/products").param("limit", "100000")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/products").param("limit", "0")).andExpect(status().isBadRequest());
    }

    @Test
    void invalidStartIsRejectedWithoutEchoingTheValue() throws Exception {
        mvc.perform(get("/api/v1/products").param("from", "0")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/products").param("from", "abc-secret"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(not(containsString("abc-secret"))));
    }

    @Test
    void unknownRouteIsDenied() throws Exception {
        mvc.perform(get("/api/v1/secret")).andExpect(status().isForbidden());
    }

    @Test
    void writeMethodsAreDenied() throws Exception {
        mvc.perform(post("/api/v1/products")).andExpect(status().isForbidden());
    }

    @Test
    void openApiDescribesBothEndpoints() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/products']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/products/{rowNumber}']").exists());
    }

    @Test
    void rowNumberLookupIsTheFirstOperationInOpenApi() throws Exception {
        String body = mvc.perform(get("/v3/api-docs")).andReturn().getResponse().getContentAsString();

        int lookup = body.indexOf("\"/api/v1/products/{rowNumber}\"");
        int list = body.indexOf("\"/api/v1/products\"");

        assertThat(lookup).isPositive();
        assertThat(lookup).isLessThan(list);
    }

    @Test
    void openApiNamesTheAuthorAndTheDataset() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.contact.name").value("Fabrizio Fatecha"))
                .andExpect(jsonPath("$.info.description").value(containsString("50,000,000 records")))
                .andExpect(jsonPath("$.info.description").value(containsString("Tech stack")));
    }

    @Test
    void openApiExplainsTheRateLimit() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.description").value(containsString("Rate limit")))
                .andExpect(jsonPath("$.info.description").value(containsString("blocked for **30 minutes**")))
                .andExpect(jsonPath("$.paths['/api/v1/products/{rowNumber}'].get.responses['429']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/products'].get.responses['429']").exists());
    }

    @Test
    void apiResponsesShowTheRemainingQuota() throws Exception {
        mvc.perform(get("/api/v1/products/{rowNumber}", 1))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-RateLimit-Remaining"));
    }

    @Test
    void swaggerEntryPointRedirectsToTheUi() throws Exception {
        mvc.perform(get("/swagger"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString("/swagger-ui/index.html")));
    }

    @Test
    void swaggerUiIsServedWithItsOwnPolicy() throws Exception {
        mvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Security-Policy", containsString("script-src 'self'")));
    }

    @Test
    void docsRejectWriteMethods() throws Exception {
        mvc.perform(post("/v3/api-docs")).andExpect(status().isForbidden());
    }
}
