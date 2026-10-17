# Password Attacks and Defences with nginx, Explained

## The idea in one sentence

nginx's limit_req counts requests for each key and answers 429 to those that arrive too fast, so a login behind it is protected only as well as the key is chosen: the real connection address holds an attacker to the limit, a client-written header does not, and a per-username key locks the owner out and misses a spray.

## The 5 acts

### 1. nginx in front of the login, with no limit

nginx sits in front of the login, which is a stub that says 401 to every password. With no limit configured, nginx
passes every request through. Twenty guesses at Dana's account are all answered: 20 answered, 0 refused. Anyone
can keep guessing for as long as they like.

### 2. limit_req on the client's address: 1r/m, burst 5

The configuration now has a zone that counts requests per client address, at one request a minute, and the login
location uses it with a burst of five. The first request is allowed. The burst lets five more through at once. That is six answered. The
remaining fourteen get 429. The rate is deliberately slow, so the count does not depend on how fast the demo runs.

### 3. The same limit, but counted on the X-Forwarded-For header

Many setups sit behind a proxy and take the client's address from the `X-Forwarded-For` header. Here the zone is
keyed on that header, and the attacker simply sends a different value with every guess. Each value has its own,
empty bucket, so all 20 guesses are answered: 20 answered, 0 refused. The limit is in place and does nothing, because
the attacker chose the key.

### 4. The same limit, counted on the real connection address

The zone is keyed on the address of the connection itself, which the client cannot choose. The attacker sends the same different header values
as before, and nginx ignores them. The result is back to 6 answered and 14 refused. Behind a trusted proxy, the real client
address must be taken from a header that only that proxy sets, and the proxy must overwrite whatever the client sent.

### 5. The bill: a limit per username

A limit per username seems natural: protect each account. An attack on Dana is held to 6 answered and 14 refused.
But the bucket for Dana is now full, and when Dana herself tries from another address, she gets 429: the
limit locks the owner out too. And a spray, with one guess at each of 20 usernames, fills no bucket at all: 20 answered, 0 refused.
A per-username limit is a lockout with the same two weaknesses as in the plain project. It belongs beside a limit per address, not in place of it.

## The verdict

Rate limit the login at the front door, on the real connection address, and take that address from a header only your
own proxy can set. Add a gentle limit per account if you must, knowing it can be used against the owner, and keep the
lockout, second factor and breach check from the plain project behind it.

## How to recognise this in code you did not write

- `limit_req_zone` and `limit_req` in an nginx configuration.
- A 429 response and a `Retry-After` header.
- A proxy configuration that sets `X-Forwarded-For` itself and discards what the client sent.
- A rate limit written in an API gateway's settings.

## Where you have already met this

- A "429 Too Many Requests" page, sometimes with a "try again later" message.
- `limit_req_zone` and `limit_req` lines in an nginx configuration.
- An API that documents its rate limits in a response header.
- Advice to "never trust X-Forwarded-For" in a security review.
