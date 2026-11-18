# Failing Closed with Spring Boot, Explained

## The idea in one sentence

A Spring Boot bank must start every decision as no, bound every outside call, undo half-done work, and tell the customer as little as the log tells support.

## The 5 acts

### 1. Failing open

The first version, OpenPaymentService, starts from yes, and a catch (Exception e) { } keeps the yes when the fraud call fails. The input is one payment, 1500.00 at unknown-electronics, sent to POST /payments
while the fraud check behaves in five ways: works and says risk high; drops the connection; answers HTTP 500; answers with a maintenance page; takes 2000 ms against a budget of 500 ms. The results:
403 refused while it works, and 200 APPROVED in all four broken cases. A check that never ran took nothing away.

### 2. Failing closed

ClosedPaymentService starts from no: yes only when the check ran, in time, and said low risk. Six payments go in: a coffee for 4.50 with the check working, the risky 1500.00 payment with the check working,
and the risky payment while the check answers HTTP 500, answers with a maintenance page and drops the connection, then a coffee while the check takes 2000 ms. The results: 200 APPROVED for the coffee,
403 refused for the fraudulent payment, and 503 with references REF-0002 to REF-0005 for the four failures. The time budget is worked out: 2 attempts x 500 ms = 1000 ms, less than the 2000 ms the
check needs, so the bank answers before the fraud check finishes. Every refusal is in the security log: 5 lines.

### 3. Failing halfway

Before: ana has 1000.00, ben has 0.00 and his account is frozen; the bank holds 1000.00. Ana sends 250.00 to ben, in four versions of the same code. With no transaction, the debit stays: ana 750.00,
total 750.00. With @Transactional and a checked exception, Spring does not roll back, because by default it rolls back on unchecked exceptions only: total 750.00. With @Transactional called from the
same class, the call does not go through Spring's proxy, so there is no transaction at all: total 750.00. With @Transactional(rollbackFor = Exception.class) called from outside, the failed credit
undoes the debit: total 1000.00. The loss, worked out: 1000.00 - 750.00 = 250.00 vanished, in 3 of 4 versions.

### 4. What the customer is told, and what the log keeps

The failure: the fraud check drops the connection. With the debugging settings on (the leaky profile, spring.web.error.include-*), Spring Boot's own error response is HTTP 500 and carries the internal
address 127.0.0.1, the full card number and code lines (.java): a map of the bank. With one @RestControllerAdvice, the same failure is HTTP 503 and carries none of them. The customer sees: the payment
could not be completed, please try again later, quoting reference REF-0001. The log line keeps the reference and the reason, fraud check unavailable, with only the card's last four digits.

### 5. The bill, and how to pay it

The outage: the fraud check needs 2000 ms and the budget is 150 ms; 10 customers each pay 4.50 for a coffee. Without a breaker, with 2 attempts per payment, 0 of 10 are approved and 10 payments x 2 =
20 calls reach the fraud check. With a breaker that opens after 3 failed payments, 0 of 10 are approved, 3 payments x 2 = 6 calls reach it, the other 7 are refused at once, and the breaker is OPEN.
The Micrometer counters by reason: breaker open 7, fraud check too slow 3; the total of 10 is at least the alert threshold of 5, so an alert is raised. After the 30 s cool-down, with the fraud check back,
one trial call approves a coffee and the breaker is CLOSED. An open breaker still says no: it fails fast, not open.

## The verdict

Start from no, put a time budget on every outside call, limit retries, open a breaker on repeated failure, annotate transfers with rollbackFor and call them from outside the class, use one error handler, switch the error details off, and alert on rising refusals.

## How to recognise this in code you did not write

- `catch (Exception e)` with an empty body.
- `@Transactional` on a private method or a method called from `this`.
- `spring.web.error.include-stacktrace=always` in a shared properties file.
- A client with no timeout.

## Where you have already met this

- `catch (Exception e) { }` and logs full of swallowed exceptions.
- `@Transactional` on a method that is called from the same class.
- A JSON error response with a `trace` field in production.
- `RestClient` or `RestTemplate` with no timeout at all.
