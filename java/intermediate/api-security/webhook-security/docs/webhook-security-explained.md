# Webhook Security: Signatures, Replays and Idempotency, Explained

## The idea in one sentence

A webhook endpoint is a public address, so every message must be verified: an HMAC signature over the signed time and exact raw body, a short time window and a record of processed
events against replays, idempotent state-checked handlers for retries and disorder, a rotated secret, and the provider's API for anything valuable.

## The 5 acts

### 1. An endpoint that trusts whatever is posted

Anyone posts a "payment succeeded" event for order A-1001 to the store's webhook address, with no signature. The store answers 200 and marks the order
paid. The provider's own records for A-1001 say: no payment. The webhook address is public, so a message to it proves nothing about who sent it.

### 2. A signature over the exact bytes

The provider signs an HMAC-SHA256 of the timestamp and the raw body with the secret it shares with the store. The real event, as signed, is accepted. The same
signature on a body changed to order A-1002 is refused with 401; a forged event with no signature is refused with 400; and one signed with a secret that is not the
provider's is refused with 401. The comparison takes the same time however many bytes match. Over the local HTTP server the answers are the same: 200, 401 and 400.

### 3. A real message sent again

With signatures only, a real refund is applied and pays back 4200 cents. Someone who saw that message sends a copy an hour later; it is still correctly signed, so it is
applied again: two refunds, 8400 cents for a 4200-cent order. With a 300-second window on the signed time and a record of processed event ids, the real refund is applied;
the same message after 30 seconds is answered "already processed, nothing changed"; an hour later it is refused as signed too long ago; and with its time edited to now
the signature no longer matches, because the time is part of what is signed. One refund, 4200 cents.

### 4. Twice, out of order, and a new secret

The provider retries the payment event, so it arrives twice: the order changes once, and A-1001 is paid. A refund for A-1002 arrives before its payment and is answered
409, "not applied: order is new, try again later", and is not recorded as processed; then the payment is applied, and the provider's retry of the refund is applied.
When the secret changes, the store accepts both for a while, so messages signed with the old and the new secret both get 200; once the old secret is removed, a message
signed with it is refused with 401.

### 5. The bill

A framework that parses the body and writes it again with other spacing, then checks the signature, refuses the real event with 401: the raw bytes must be kept, and checked,
before anything parses them. The real event is accepted. Before shipping, the store asks the provider's API about A-1001, and the answer, payment.succeeded 4200, matches the event.
The shared secret lives outside the code, is changed on a schedule with a changeover, and a leak is a forged-payment risk. Events can arrive late, twice or out of order, so every
handler must be idempotent and check the order's state.

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
