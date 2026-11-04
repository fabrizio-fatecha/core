#!/bin/sh
set -eu

psql -v ON_ERROR_STOP=1 -v ro_password="$APP_DB_RO_PASSWORD" --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<'SQL'
CREATE ROLE app_ro LOGIN PASSWORD :'ro_password' CONNECTION LIMIT 20;

REVOKE ALL ON SCHEMA public FROM PUBLIC;
GRANT USAGE ON SCHEMA public TO app_ro;
GRANT SELECT (row_number, sku, name, price, stock_qty, category_id, brand_id) ON products TO app_ro;

ALTER ROLE app_ro SET statement_timeout = '3s';
ALTER ROLE app_ro SET lock_timeout = '1s';
ALTER ROLE app_ro SET idle_in_transaction_session_timeout = '5s';
SQL
