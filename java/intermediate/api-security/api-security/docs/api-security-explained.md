# API Security: Mass Assignment, Data Exposure and Resource Limits, Explained

## The idea in one sentence

An API should say which fields a caller may set, which fields it may see, and how much it may ask for, in code, and not leave those decisions to whatever the database record happens to contain.

## The 5 acts

### 1. A profile call that works

Ana's app sends PATCH /accounts/acc-0001 with `{"displayName": "Ana Ruiz"}`. The API answers 200 with the account record as it is stored: displayName, email, role, balanceCents, creditLimitCents, riskScore,
cardNumber and pinHash. The profile screen shows one field; the response carries eight. Nothing looks wrong, which is why the flaws survive testing.

### 2. Whatever the caller sends becomes a field

Ana adds two keys by hand: `"role": "admin"` and `"creditLimitCents": 100000000`. Before the call her role is customer and her limit is 50000 cents. The API copies every key it receives onto the record, so after the call
she is an admin with a limit of 100000000 cents, a million dollars. The API never decided which fields were hers to set; it copied what it was given. This is mass assignment.

### 3. The response is the whole record, and a list has no end

GET /accounts/acc-0001 returns the entity, so the full card number 4242 4242 4242 0001, the PIN hash and the internal risk score travel to the phone: anything stored becomes anything shown.
The branch directory takes its page size from the caller, so `size=1000000` returns all 300 accounts with 8 fields each, 2400 values. A loop of 1000 requests is answered 1000 times, and nothing slows it down.

### 4. Explicit models and a page cap

The fixed API reads the JSON into `UpdateProfileRequest`, which names two fields, displayName and email, with a type and a length for each. The same hand-made request is checked key by key: displayName is named, role and
creditLimitCents are not, so the answer is 400 unknown field and the record still says customer and 50000.
A request that only changes the display name gets 200 and an `AccountView` with five fields: id, displayName, email, balanceCents and cardLast4, the card shown as ****0001. No PIN hash, no risk score, no role.
The directory caps the page size: min(1000000, 50) = 50, and each row is a `DirectoryEntry` with only an id and a display name.

### 5. The budget runs out, and refills

Each caller has a token bucket for reads: it holds 100 tokens and refills 100 a minute, 1.67 a second. One account costs 1 token and a list costs 1 per row, so Ana's earlier calls used 1 + 50 and left her 100 - 1 - 50 = 49.
Of the next 1000 requests, made at the same instant, 49 are answered and 951 get 429 Too Many Requests with `Retry-After: 1`. The wait is the missing token divided by the refill rate, rounded up: ceil((1 - 0.00) / 1.67) = ceil(0.60) = 1 second.
One second later the bucket holds 0.00 + 1.67 = 1.67 tokens, so the call is answered and 0.67 are left. user2 has her own bucket: a list of 5 leaves her 95.

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
