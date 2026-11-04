package com.core.product;

import java.math.BigDecimal;

record ProductRow(long rowNumber, String sku, String name, BigDecimal price, long categoryId, int stockQty) {
}
