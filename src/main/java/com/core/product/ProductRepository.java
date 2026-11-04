package com.core.product;

import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class ProductRepository {

    private final JdbcClient jdbc;

    ProductRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    Optional<ProductRow> findByRowNumber(long rowNumber) {
        return jdbc.sql("""
                        SELECT row_number, sku, name, price, category_id, stock_qty
                          FROM products
                         WHERE row_number = :rowNumber
                        """)
                .param("rowNumber", rowNumber)
                .query(ProductRow.class)
                .optional();
    }

    List<ProductRow> fromRowNumber(long from, int limit) {
        return jdbc.sql("""
                        SELECT row_number, sku, name, price, category_id, stock_qty
                          FROM products
                         WHERE row_number >= :from
                         ORDER BY row_number
                         LIMIT :limit
                        """)
                .param("from", from)
                .param("limit", limit)
                .query(ProductRow.class)
                .list();
    }
}
