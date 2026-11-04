package com.core.product;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/products")
class ProductController {

    private final ProductService service;

    ProductController(ProductService service) {
        this.service = service;
    }

    @Operation(summary = "Get one product by its row number (1 to 50,000,000)")
    @GetMapping(path = "/{rowNumber}", produces = MediaType.APPLICATION_JSON_VALUE)
    ProductDto get(@PathVariable @Positive long rowNumber) {
        return service.get(rowNumber);
    }

    @Operation(summary = "Walk through the products in row-number order, starting at a given row number")
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    ProductPage list(@RequestParam(defaultValue = "1") @Positive long from,
                     @RequestParam(defaultValue = "25") @Min(1) @Max(100) int limit) {
        return service.list(from, limit);
    }
}
