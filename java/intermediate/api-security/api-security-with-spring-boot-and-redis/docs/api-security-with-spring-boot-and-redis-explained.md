# API Security with Spring Boot and Redis, Explained

## The idea in one sentence

An API names the fields it accepts, names the fields it returns, caps what a caller may ask for, and keeps the count of requests where every server can see it.

## The 5 acts

### 1. The first version

The first controller binds the body to the stored `Account` with `@RequestBody Account patch`. Ana's app sends `{"displayName":"Ana K"}` and gets back the stored record: nine fields, among them the card number 4111 1111 1111 0001, the PIN hash, the risk score and the role.
With two keys added by hand, `{"role":"admin","creditLimitCents":100000000}`, Ana is an admin with a limit of 100000000 cents. A page size of 1000000 returns 300 accounts x 9 fields = 2700 values, and 1000 requests in a loop are all answered.

### 2. A request model

The second controller binds the body to `UpdateProfileRequest`, a record with two fields and their limits: displayName with `@Size(min = 1, max = 40)`, email with `@Email @Size(max = 80)`. The property `spring.jackson.deserialization.fail-on-unknown-properties=true` turns any other field into an error.
A new display name gets 200 and an `AccountView`. The role and credit limit body gets 400 unknown field; a bad e-mail, a 41-letter name and an empty name get 400 invalid; a body that is not JSON gets 400 unreadable body. The record still says customer and 50000.

### 3. A response model and a page ceiling

The controller returns an `AccountView` record instead of the stored account: id, displayName, email, balanceCents and cardLast4 = ****0001. No role, no limit, no risk score, no PIN hash.
The directory caps the page with `bank.page.max=50`: min(1000000, 50) = 50 entries, each a `DirectoryEntry` of id and display name. `@Min(1)` refuses size=0 with 400 invalid parameter, and a request with no login gets 401 from Spring Security before any controller runs.

### 4. The budget runs out, and refills

A filter takes one token from the signed-in caller's bucket before the controller runs. The bucket lives in Redis under `rate:<caller>`, holds 50 tokens and refills 1 a second, and a Lua script reads, refills, takes and writes it back in one step.
1000 requests at one moment: 50 are answered, 950 get 429 with `Retry-After: 1`. The wait is the missing token over the refill rate: ceil((1 - 0) / 0.001) = 1000 ms, and the header is ceil(1000 / 1000) = 1 second.
One second later the bucket holds 0 + 1 = 1 token, the call is answered and 0 are left. Ben has his own bucket; Redis holds the keys rate:ana and rate:ben.

### 5. The bill

Two servers with a budget each in their own memory answer 50 + 50 = 100 of 1000 requests that alternate between them: each server's budget is a budget per server, not per caller. With the budget in Redis, the same 1000 requests get 25 + 25 = 50.
Redis is now part of every request. When its container is paused, the API refuses with 503 rather than run with no budget; when Redis answers again, so does the API. A budget protects the service, not the data: it does not replace the request or the response model.

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
