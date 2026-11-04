package com.core.product;

import java.util.List;

public record ProductPage(List<ProductDto> items, Long nextRowNumber) {
}
