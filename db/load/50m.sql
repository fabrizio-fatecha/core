\set ON_ERROR_STOP on
\timing on
SET synchronous_commit = off;

INSERT INTO products (row_number, sku, name, price, stock_qty, category_id, brand_id, cost, supplier_id)
SELECT g, 'SKU-' || lpad(g::text, 8, '0'), 'Product ' || g, round((random() * 990 + 10)::numeric, 2),
       (random() * 500)::int, (g % 20) + 1, (g % 50) + 1, round((random() * 500 + 5)::numeric, 2), (g % 30) + 1
  FROM generate_series(100001, 5000000) AS g;
\echo inserted 1/10

INSERT INTO products (row_number, sku, name, price, stock_qty, category_id, brand_id, cost, supplier_id)
SELECT g, 'SKU-' || lpad(g::text, 8, '0'), 'Product ' || g, round((random() * 990 + 10)::numeric, 2),
       (random() * 500)::int, (g % 20) + 1, (g % 50) + 1, round((random() * 500 + 5)::numeric, 2), (g % 30) + 1
  FROM generate_series(5000001, 10000000) AS g;
\echo inserted 2/10

INSERT INTO products (row_number, sku, name, price, stock_qty, category_id, brand_id, cost, supplier_id)
SELECT g, 'SKU-' || lpad(g::text, 8, '0'), 'Product ' || g, round((random() * 990 + 10)::numeric, 2),
       (random() * 500)::int, (g % 20) + 1, (g % 50) + 1, round((random() * 500 + 5)::numeric, 2), (g % 30) + 1
  FROM generate_series(10000001, 15000000) AS g;
\echo inserted 3/10

INSERT INTO products (row_number, sku, name, price, stock_qty, category_id, brand_id, cost, supplier_id)
SELECT g, 'SKU-' || lpad(g::text, 8, '0'), 'Product ' || g, round((random() * 990 + 10)::numeric, 2),
       (random() * 500)::int, (g % 20) + 1, (g % 50) + 1, round((random() * 500 + 5)::numeric, 2), (g % 30) + 1
  FROM generate_series(15000001, 20000000) AS g;
\echo inserted 4/10

INSERT INTO products (row_number, sku, name, price, stock_qty, category_id, brand_id, cost, supplier_id)
SELECT g, 'SKU-' || lpad(g::text, 8, '0'), 'Product ' || g, round((random() * 990 + 10)::numeric, 2),
       (random() * 500)::int, (g % 20) + 1, (g % 50) + 1, round((random() * 500 + 5)::numeric, 2), (g % 30) + 1
  FROM generate_series(20000001, 25000000) AS g;
\echo inserted 5/10

INSERT INTO products (row_number, sku, name, price, stock_qty, category_id, brand_id, cost, supplier_id)
SELECT g, 'SKU-' || lpad(g::text, 8, '0'), 'Product ' || g, round((random() * 990 + 10)::numeric, 2),
       (random() * 500)::int, (g % 20) + 1, (g % 50) + 1, round((random() * 500 + 5)::numeric, 2), (g % 30) + 1
  FROM generate_series(25000001, 30000000) AS g;
\echo inserted 6/10

INSERT INTO products (row_number, sku, name, price, stock_qty, category_id, brand_id, cost, supplier_id)
SELECT g, 'SKU-' || lpad(g::text, 8, '0'), 'Product ' || g, round((random() * 990 + 10)::numeric, 2),
       (random() * 500)::int, (g % 20) + 1, (g % 50) + 1, round((random() * 500 + 5)::numeric, 2), (g % 30) + 1
  FROM generate_series(30000001, 35000000) AS g;
\echo inserted 7/10

INSERT INTO products (row_number, sku, name, price, stock_qty, category_id, brand_id, cost, supplier_id)
SELECT g, 'SKU-' || lpad(g::text, 8, '0'), 'Product ' || g, round((random() * 990 + 10)::numeric, 2),
       (random() * 500)::int, (g % 20) + 1, (g % 50) + 1, round((random() * 500 + 5)::numeric, 2), (g % 30) + 1
  FROM generate_series(35000001, 40000000) AS g;
\echo inserted 8/10

INSERT INTO products (row_number, sku, name, price, stock_qty, category_id, brand_id, cost, supplier_id)
SELECT g, 'SKU-' || lpad(g::text, 8, '0'), 'Product ' || g, round((random() * 990 + 10)::numeric, 2),
       (random() * 500)::int, (g % 20) + 1, (g % 50) + 1, round((random() * 500 + 5)::numeric, 2), (g % 30) + 1
  FROM generate_series(40000001, 45000000) AS g;
\echo inserted 9/10

INSERT INTO products (row_number, sku, name, price, stock_qty, category_id, brand_id, cost, supplier_id)
SELECT g, 'SKU-' || lpad(g::text, 8, '0'), 'Product ' || g, round((random() * 990 + 10)::numeric, 2),
       (random() * 500)::int, (g % 20) + 1, (g % 50) + 1, round((random() * 500 + 5)::numeric, 2), (g % 30) + 1
  FROM generate_series(45000001, 50000000) AS g;
\echo inserted 10/10

VACUUM (ANALYZE) products;
\echo load finished
