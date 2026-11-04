# core

A small API with a big table: **50 million products, and every one of them has a plain number.**

Ask for product `31,415,926` and you get it back. Ask for a page starting at `1,000,000` and you walk through the catalog in order, a hundred at a time. No hashes, no UUIDs, no tokens to decode: the number *is* the id.

I built it as a personal project to see how fast, simple and safe a Spring Boot API can be when the table underneath is huge, and to run it the way I would run a real service: in Docker, on one small server.

## Try it

The API is live. Open the interactive documentation and call it right from your browser:

**https://api.fatecha.com.py/swagger**

Or from a terminal:

```bash
curl https://api.fatecha.com.py/api/v1/products/77
```

It is public and has no login, so each IP gets a small number of requests (see [Fair use](#fair-use)).

## What it does

| Endpoint | What you get |
|---|---|
| `GET /api/v1/products/{rowNumber}` | One product by its number, from 1 to 50,000,000 |
| `GET /api/v1/products?from=1&limit=25` | A page of products in order, starting at a number (`limit` goes from 1 to 100) |

```bash
curl http://localhost/api/v1/products/77
```

```json
{"rowNumber":77,"sku":"SKU-00000077","name":"Product 77","price":820.14,"categoryId":18,"inStock":true}
```

A page also tells you where the next one starts, so walking the whole catalog is a loop:

```json
{"items":[ ... ],"nextRowNumber":101}
```

On the last page `nextRowNumber` is `null`. There is interactive documentation at `/swagger`.

## Fair use

The API is public and has no login, so it protects itself: **each IP address can make 10 requests, and after that it is blocked for 30 minutes** (HTTP 429 with a `Retry-After` header). Every API response shows how many requests you have left in `X-RateLimit-Remaining`. Opening the documentation does not count.

## How fast is it?

In my own measurements, from my laptop to a small server (2 GB of RAM) in São Paulo, a lookup anywhere in the 50 million rows took about 46 ms in total. About 35 ms of that was only the network trip, which leaves roughly 10 ms for nginx, the application and the database together.

## How it's built

- **Java 21 and Spring Boot 4.** Virtual threads, records, and no session state, so it could run as many copies as needed.
- **PostgreSQL 17.** The row number is the primary key and the only index. Pages use `WHERE row_number >= :from`, never `OFFSET` and never `COUNT(*)`, so page one million costs the same as page one.
- **A careful database user.** The app connects with a read-only role that can see only the columns it needs, and any query is cut after 3 seconds.
- **Deny by default.** Only `GET` on the documented routes is allowed. Errors come back as clean JSON, with no stack traces.
- **nginx in front,** with per-IP limits, and everything runs with Docker Compose from a minimal non-root image.

```
src/main/java/com/core/
  product/      the two endpoints, their service and their SQL
  ratelimit/    the per-IP quota
  config/       security, and the text you see in Swagger
  error/        clean JSON errors
  resilience/   a cap on simultaneous database calls
db/             schema, database roles, sample data and the 50M loader
nginx/          the reverse proxy and its limits
```

## Run it

```bash
cp .env.example .env
```

Fill in `POSTGRES_PASSWORD` and `APP_DB_RO_PASSWORD` (for example with `openssl rand -hex 24`), then:

```bash
docker compose up --build
curl http://localhost/api/v1/products/77
```

Locally the database starts with 100,000 products. To grow it to 50 million (a few minutes and about 7 GB of disk):

```bash
docker compose exec -T db psql -U postgres -d core < db/load/50m.sql
```

## Tests

```bash
docker run --rm -v "$PWD":/src -w /src -v /var/run/docker.sock:/var/run/docker.sock maven:3.9-eclipse-temurin-21 mvn -B verify
```

There are 26 tests: 20 run the whole application against a real PostgreSQL in a container, and 6 cover the request quota. On Docker Desktop for Mac, add `-e TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal`.

## Deploying

```bash
DEPLOY_HOST=<server ip> ./deploy.sh
```

It builds the image, sends it over SSH, and starts it on the server. The first time it also creates the server's `.env` with random passwords. The script builds for ARM64 (AWS Graviton); change `--platform` in it for x86.

## More

- [Use cases](docs/use-cases.md): what a visitor and the person running it can do, and which tests cover each case.

Made by Fabrizio Fatecha.
