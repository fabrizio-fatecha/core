CREATE TABLE products (
    row_number  bigint        PRIMARY KEY,
    sku         varchar(32)   NOT NULL,
    name        varchar(200)  NOT NULL,
    price       numeric(12,2) NOT NULL CHECK (price >= 0),
    stock_qty   integer       NOT NULL DEFAULT 0 CHECK (stock_qty >= 0),
    category_id bigint        NOT NULL,
    brand_id    bigint        NOT NULL,
    cost        numeric(12,2) NOT NULL,
    supplier_id bigint        NOT NULL
);
