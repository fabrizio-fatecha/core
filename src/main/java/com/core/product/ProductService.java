package com.core.product;

import com.core.error.NotFoundException;
import com.core.resilience.DbBulkhead;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
class ProductService {

    private final ProductRepository repository;
    private final DbBulkhead bulkhead;

    ProductService(ProductRepository repository, DbBulkhead bulkhead) {
        this.repository = repository;
        this.bulkhead = bulkhead;
    }

    ProductDto get(long rowNumber) {
        return bulkhead.call(() -> repository.findByRowNumber(rowNumber))
                .map(ProductDto::from)
                .orElseThrow(NotFoundException::new);
    }

    ProductPage list(long from, int limit) {
        List<ProductRow> rows = bulkhead.call(() -> repository.fromRowNumber(from, limit + 1));

        boolean hasNext = rows.size() > limit;
        List<ProductRow> page = hasNext ? rows.subList(0, limit) : rows;
        Long next = hasNext ? rows.get(limit).rowNumber() : null;

        return new ProductPage(page.stream().map(ProductDto::from).toList(), next);
    }
}
