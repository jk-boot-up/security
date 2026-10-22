# API Security with Spring Boot and Redis, Explained

## The idea in one sentence

An API names the fields it accepts, names the fields it returns, caps what a caller may ask for, and keeps the count of requests where every server can see it.

## The 5 acts

### 1. The first version

The PATCH of displayName works, with role customer and a credit limit of 50000. Whatever the caller sends becomes a field: a body with role admin and a credit limit of 100000000 is applied and the answer has role admin. The record comes back whole, with nine fields including the card number and the PIN hash. A page size of 1000000 returns 300 accounts, every field. A thousand requests in a loop are all answered, none slowed down.

### 2. A request model

An UpdateProfileRequest record names displayName and email. The same displayName update returns the five-field view. The body with role and creditLimitCents gets 400 unknown field, and afterwards the role is customer and the credit limit 50000. An email that is not an email, a name of 41 letters and an empty name are each a 400 with the field named, and a body that is not JSON is a 400 unreadable body.

### 3. A response model and a page ceiling

GET /accounts/acc-0001 returns id, displayName, email, balanceCents and cardLast4, with the card as ****0001: no role, no limit, no risk score, no PIN hash. GET /accounts?size=1000000 returns 50 accounts with id and displayName only. A size of 0 is a 400 invalid parameter, and a request with no login is a 401.

### 4. The budget runs out, and refills

Each signed-in caller has a bucket of 50 tokens that refills at one a second. A thousand requests in a loop get 50 answered, then 429 with Retry-After 1s. After the clock moves on by one second, one more request is answered. Ben, meanwhile, gets his own full bucket and a page of 5 accounts. The buckets are the keys rate:ana and rate:ben in Redis, and the script that takes a token runs in one step so two servers cannot take the same one.

### 5. The bill

Two servers, each with its own budget in memory: of 1000 requests alternating between them, 100 are answered, a budget per server and not per caller. With the budget in Redis, 50 are answered. When Redis stops answering, the API refuses with 503 rather than run without a budget, and answers again when Redis does. A budget protects the service and the other callers, not the data: it does not replace the request model or the response model.

## The verdict

Never bind a body to a stored record or return one; use a request record and a response record, set fail-on-unknown-properties, validate values, cap every page, budget every caller in a store shared by every server, and decide in writing what the API does when the counter is down.

## How to recognise this in code you did not write

- `@RequestBody` on an entity class.
- A JSON response with a `password` or `token` field.
- A `size` parameter with no maximum.
- A `Retry-After` header in a client's retry code.

## Where you have already met this

- `@RequestBody` on an entity or a JPA class.
- A response that includes `password`, `ssn` or a token field.
- `?size=1000000` in a pen test report.
- `429 Too Many Requests` and a `Retry-After` header.
