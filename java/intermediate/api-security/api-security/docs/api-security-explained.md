# API Security: Mass Assignment, Data Exposure and Resource Limits, Explained

## The idea in one sentence

An API should say which fields a caller may set, which fields it may see, and how much it may ask for, in code, and not leave those decisions to whatever the database record happens to contain.

## The 5 acts

### 1. A profile call that works

Ana's app sends `{"displayName": "Ana Ruiz"}` and the API answers 200 with the updated account. Nothing looks wrong, but look at what came back: eight fields.

### 2. Whatever the caller sends becomes a field

The same call with two extra keys, `role` and `creditLimitCents`, copies them onto the account. Ana is now an admin with a credit limit of 100,000,000 cents. The API never decided that these fields were the caller's to change;
it only copied what it was given.

### 3. The response is the whole record, and a list has no end

The API returns the account entity: the full card number, the PIN hash, the risk score. Anything stored becomes anything shown. The branch directory takes the page size from the caller, so size=1,000,000 returns
all 300 accounts with every field. A loop of 1000 requests is answered 1000 times without slowing down.

### 4. Explicit models, a page cap and a budget

The fixed API reads a request into `UpdateProfileRequest` with two named fields, checks their type and length, and refuses anything else with 400 and no change: Ana's role afterwards is still customer. The response is an
`AccountView` with five fields and the card as ****0001. The list is capped at 50 whatever the caller asks for, and shows the directory view: ids and display names, not balances or cards.

### 5. The budget runs out, and refills

Each caller has a token bucket: a burst of 100 rows, refilled at 100 a minute, and a list costs a token per row. The earlier calls used 51 of them; of the next 1000 requests, 49 are answered, and then the API says 429
with `Retry-After: 1`. After waiting that long the call works again. Another customer has their own bucket and is not affected.

## The verdict

Write a request model for every write, refuse unknown fields, write a response model for every read, never return an entity, cap page sizes, and put a budget on every caller and every expensive or business-sensitive call.
Return 429 with `Retry-After` and keep the numbers in configuration.

## How to recognise this in code you did not write

- `BeanUtils.copyProperties(request, entity)` or `objectMapper.readerForUpdating(entity)`.
- Controllers that return repository entities.
- `@RequestParam int size` with no maximum.
- Findings with API3, API4 or API6.

## Where you have already met this

- A `PATCH` that copies a JSON map onto an entity with a mapper or `BeanUtils.copyProperties`.
- `return repository.findAll()` and `return entity` in a controller.
- A `limit` or `pageSize` parameter with no upper bound.
- HTTP 429 and the `Retry-After` header.
