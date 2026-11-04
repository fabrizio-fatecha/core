package com.core.product;

import java.math.BigDecimal;

public record ProductDto(long rowNumber, String sku, String name, BigDecimal price, long categoryId, boolean inStock) {

    static ProductDto from(ProductRow row) {
        return new ProductDto(row.rowNumber(), row.sku(), row.name(), row.price(), row.categoryId(), row.stockQty() > 0);
    }
}
