# Failing Closed with Spring Boot, Explained

## The idea in one sentence

A Spring Boot bank must start every decision as no, bound every outside call, undo half-done work, and tell the customer as little as the log tells support.

## The 5 acts

### 1. Failing open

The risky payment is refused while the fraud check works. Then the fraud service is told to drop the connection, answer 500, answer with a maintenance page, and take 2 seconds against a budget of 500 ms. Each time the catch-all swallows the exception and the payment is approved. The decision started as yes, and a check that never ran took nothing away.

### 2. Failing closed

A coffee is approved when the check ran and said low risk. The risky payment is refused with 403 when the check works. When the fraud check answers 500, answers with a maintenance page or drops the connection, the payment is refused with 503 and a reference such as REF-0002. A coffee while the fraud check takes 2 seconds against a budget of 500 ms is refused too, and the answer comes before the fraud check finishes. Every refusal is in the log with its reference: 5 lines.

### 3. Failing halfway

Ana sends 250.00 to Ben, whose account is frozen. Without a transaction the debit stays: Ana has 750.00 and the bank is 250.00 short. With `@Transactional` and a checked exception the debit stays as well, because Spring rolls back on unchecked exceptions only. Called from inside the same class, the proxy is skipped and the debit stays again. With `@Transactional(rollbackFor = Exception.class)` called from outside, Ana keeps 1000.00.

### 4. What the customer is told

With the debugging profile the 500 response names the internal server, shows the full card number and shows code lines. With one `@RestControllerAdvice` the same failure gives 503 and a short message with a reference, and none of those three. The log line has the reference, the reason and the last four digits, and not the full card number, so support can find the detail from the reference.

### 5. The bill, and how to pay it

Failing closed costs availability: ten coffees while the fraud check is slow are all refused, and with a retry each, 20 calls reach the fraud check. With a breaker that opens after 3 failed payments, 6 calls reach it and the other 7 payments are refused at once. The refusal counters give {fraud check too slow=3, fraud check unavailable, breaker open=7} and an alert is raised. After the cool-down, with the fraud check back, a coffee is approved and the breaker is closed again.

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
