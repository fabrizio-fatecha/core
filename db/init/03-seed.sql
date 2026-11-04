INSERT INTO products (row_number, sku, name, price, stock_qty, category_id, brand_id, cost, supplier_id)
SELECT g,
       'SKU-' || lpad(g::text, 8, '0'),
       'Product ' || g,
       round((random() * 990 + 10)::numeric, 2),
       (random() * 500)::int,
       (g % 20) + 1,
       (g % 50) + 1,
       round((random() * 500 + 5)::numeric, 2),
       (g % 30) + 1
  FROM generate_series(1, 100000) AS g;

ANALYZE products;
