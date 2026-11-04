# Use cases

Two kinds of people use this system.

- **Visitor:** anyone who calls the API or opens its documentation. There are no accounts or logins.
- **Operator:** the person who deploys and maintains it.

The API only reads. Nothing can be created, changed or deleted through it.

## Visitor

### UC-1. Look up one product by its number

**Goal:** get a single product when you know its number.

**Request:** `GET /api/v1/products/{rowNumber}`

| Situation | Result |
|---|---|
| A product has that number (1 to 50,000,000) | `200` with `rowNumber`, `sku`, `name`, `price`, `categoryId` and `inStock` |
| No product has that number, for example 60,000,000 | `404` |
| The number is not a positive whole number (`0`, `-5`, `abc`) | `400` |

Internal data never leaves the API: cost, supplier and the exact stock count are not part of the answer, only whether the product is in stock.

**Tests:** `getByRowNumberReturnsTheMatchingProduct`, `getFirstAndLastRowNumbersOfTheSeed`, `getByUnknownRowNumberIsNotFound`, `getByInvalidRowNumberIsBadRequest`.

### UC-2. Walk through the catalog in order

**Goal:** read the products one page after another.

**Request:** `GET /api/v1/products?from=1&limit=25`

- `from` is the number to start at. It defaults to `1`.
- `limit` is the page size, from 1 to 100. It defaults to `25`.
- The answer has `items` and `nextRowNumber`. To read the next page, send that number as `from`.
- On the last page `nextRowNumber` is `null`.
- A `limit` above 100, a `limit` below 1, or a `from` that is not a positive whole number gets a `400`, and the error never repeats what you sent.

**Tests:** `listStartsAtRowNumberOneByDefault`, `listContinuesFromTheGivenRowNumber`, `listEndsWithoutNextRowNumber`, `listDefaultsToTwentyFiveItems`, `pageSizeIsCapped`, `invalidStartIsRejectedWithoutEchoingTheValue`.

### UC-3. Read and try the documentation

**Goal:** learn what the API does and call it from the browser.

- `/swagger` opens Swagger UI, and `/v3/api-docs` is the machine-readable specification.
- The lookup by number is listed first.
- The description names the author, says the catalog has 50,000,000 records, lists the technology behind it, and explains the request limit.
- Both operations document the `429` answer.
- "Try it out" sends real requests, and those count towards the limit. Loading the documentation itself does not.

**Tests:** `swaggerEntryPointRedirectsToTheUi`, `swaggerUiIsServedWithItsOwnPolicy`, `openApiDescribesBothEndpoints`, `rowNumberLookupIsTheFirstOperationInOpenApi`, `openApiNamesTheAuthorAndTheDataset`, `openApiExplainsTheRateLimit`.

### UC-4. Reach the request limit

**Goal:** know what happens when an IP asks too much, and when it can ask again.

- Each IP address can make **10 requests** to `/api/`.
- Every answer carries `X-RateLimit-Remaining`, counting down from 9 to 0.
- The next request is rejected with `429`, a JSON body, and a `Retry-After` header with the seconds left.
- The block lasts **30 minutes from the tenth request**. Requests made while blocked do not extend it.
- After that the IP gets a fresh 10. If an IP never reaches 10, its count starts over 30 minutes after its first request.
- Every IP has its own count. People behind the same address share it.
- nginx adds a second layer: more than 10 requests per second from one IP (with a burst of 20) also gets a `429`.

**Tests:** `firstTenRequestsAreAdmittedAndTheEleventhIsRejected`, `theBlockLastsThirtyMinutesFromTheTenthRequest`, `rejectedRequestsDoNotExtendTheBlock`, `eachAddressHasItsOwnQuota`, `onlyApiRequestsCountTowardsTheQuota`, `theCountStartsOverWhenTheLimitIsNeverReached`, `apiResponsesShowTheRemainingQuota`.

### UC-5. Send something the API does not accept

**Goal:** get a clear, safe answer when the request is wrong.

| Request | Result |
|---|---|
| A route that does not exist | `403` |
| `POST`, `PUT`, `PATCH` or `DELETE` | `403` |
| Invalid values in the URL | `400`, without echoing the value back |
| An unexpected failure inside the system | `500` with an `errorId` to find it in the logs, and no internal details |

**Tests:** `unknownRouteIsDenied`, `writeMethodsAreDenied`, `docsRejectWriteMethods`, `invalidStartIsRejectedWithoutEchoingTheValue`, `getByInvalidRowNumberIsBadRequest`.

## Operator

### UC-6. Deploy a new version

```bash
DEPLOY_HOST=<server ip> ./deploy.sh
```

The script builds the image, sends it over SSH, copies the configuration, creates the server's `.env` with random passwords the first time, starts the containers and reloads nginx. Restarting the API resets every IP's request count.

Not covered by automated tests.

### UC-7. Grow the data to 50 million products

A fresh database starts with 100,000 products from `db/init`. Running `db/load/50m.sql` inserts the rest in ten batches and finishes with a `VACUUM ANALYZE`. The table takes about 5.5 GB plus 1 GB for the primary key.

Not covered by automated tests.

## What the automated tests do not cover

These were checked by hand against the real servers, not by the test suite:

- The database role: that it cannot read the hidden columns or write, and that it cuts queries after 3 seconds. The tests connect as the database owner.
- The nginx limit of 10 requests per second.
- The cap of 10 simultaneous database calls, which answers `503` when it is exceeded.
- HTTPS and certificates.
