# Webhook Security: Signatures, Replays and Idempotency, Explained

## The idea in one sentence

A webhook endpoint is a public address, so every message must be verified: an HMAC signature over the signed time and exact raw body, a short time window and a record of processed
events against replays, idempotent state-checked handlers for retries and disorder, a rotated secret, and the provider's API for anything valuable.

## The 6 acts

### 1. An endpoint that trusts whatever is posted

The request: anyone posts {"id":"evt_forged1","type":"payment.succeeded","order":"A-1001","amount":4200} to POST /webhooks/payments, with no signature.
The response: 200 order A-1001 is now paid. The provider's own records for A-1001 say: no payment. The order is marked paid and was never paid.
The webhook address is public, so a message to it proves nothing about who sent it.

### 2. How the provider signs a message

The inputs: the shared secret whsec-demo-shop-2026 (20 bytes), the time t = 1800000000 seconds since 1 January 1970, and the raw body of 79 bytes.
Step 1: what is signed is t, a dot, and the raw body: 1800000000.{"id":"evt_1001paid",...}. Step 2: the secret is padded with zeros to one 64-byte block; each byte
XOR 36 gives the inner key, each byte XOR 5c the outer key. Step 3: inner = SHA-256(inner key + what is signed), and v1 = SHA-256(outer key + inner).
The header sent with the body is Shop-Signature: t=1800000000,v1=dc14c6ef9d7ef5d1fd993788607f660adf0d0e8e177d6a0e2f5748dcce6c5cc3. The hand-made value equals the JDK's Mac.

### 3. The store checks the signature

The store's steps: read t and v1 from the header, compute the HMAC of t, a dot and the raw body received, and compare it with v1 in constant time (MessageDigest.isEqual).
The real event, as signed: v1 sent dc14c6ef9d7ef5d1..., computed dc14c6ef9d7ef5d1..., 200 order A-1001 is now paid. The body changed to A-1002 with the same header:
computed 8dfb52122bf9f611..., 401 refused: the signature does not match. A forged event with no header: 400 refused: no signature. A forged event signed with another
secret: 401. Over the local HTTP server (POST only, at most 4096 bytes) the answers are the same: 200, 401, 400.

### 4. A real message sent again

The message: a real refund for A-1001, signed at t=1800000000. With signatures only, the real refund at t + 0 is applied, and a copy sent again at t + 3600 is applied too:
two refunds, 8400 cents for a 4200-cent order. With a 300-second window on the signed time and a record of processed event ids: the real refund is applied; the copy at
t + 30 (age 30 <= 300, id already seen) gets 200 already processed, nothing changed; the copy at t + 3630 (age 3630 > 300) gets 400 refused: signed too long ago; and the
copy with its t edited to 1800003630 gets 401, because the time is part of what is signed. One refund, 4200 cents.

### 5. Twice, out of order, and a new secret

The events arrive in this order: the payment for A-1001, the same payment again (a retry), a refund for A-1002 before its payment, the payment for A-1002, and the refund again
(a retry). The responses, with each order's state before: new, 200 paid; paid, 200 already processed; new, 409 not applied: order is new, try again later; new, 200 paid;
paid, 200 refunded, 1999 cents paid back. The orders changed 3 times. To change the secret, the store accepts whsec-demo-shop-2026 and whsec-demo-shop-2027 during a
changeover: messages signed with either get 200. After the old one is removed, a message signed with it gets 401.

### 6. The bill

The raw body, as signed, is 79 bytes. The same event, parsed and written again with other spacing, is 88 bytes. With the same header, whose v1 is dc14c6ef9d7ef5d1...,
the re-written body computes 909d1bd03dc6ecc1... and is refused with 401; the raw body computes dc14c6ef9d7ef5d1... and gets 200. So the raw bytes must be kept, and checked,
before anything parses them. Before shipping, the store asks the provider's API about A-1001: payment.succeeded 4200, which matches the event. The secret lives outside the code
and is changed on a schedule; events come late, twice or out of order, so handlers are idempotent.

## The verdict

Verify the provider's signature on the raw body before parsing, with a constant-time comparison. Refuse messages outside a few minutes, remember processed event ids, and make
every handler idempotent. Keep the secret in a secret store and rotate it with an overlap. For high-value actions, confirm with the provider's API before acting.

## How to recognise this in code you did not write

- A webhook handler that reads the raw request body before deserializing it.
- Signature headers with a `t=` timestamp and a `v1=` HMAC.
- Tables of processed event ids with an expiry.
- Security findings titled "missing webhook signature verification", CWE-345.

## Where you have already met this

- `Stripe-Signature`, `X-Hub-Signature-256` and similar headers on webhooks from payment providers and code hosts.
- Webhook settings pages that show a signing secret and a "roll secret" button.
- Delivery logs that show the same event retried several times.
- Incident reports where orders were marked paid by forged callbacks.
