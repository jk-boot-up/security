# CORS with Spring Boot, Explained

## The idea in one sentence

CORS decides which pages may read an answer from the browser, and Spring Boot enforces the list you give it, so the list must be exact and registered before the login check.

## The 5 acts

### 1. No policy: a page may send, but not read

The bank's own page reads /account. A page on evil.example reads /account with the customer's credentials: the request reaches the bank and the answer is blocked. It POSTs /transfer as a simple request: it reaches the bank and the bank processes it. Its PUT with JSON needs a preflight, which gets no approval, so it never reaches the bank. The bank processed [GET /account, POST /transfer]: CORS did not stop the POST.

### 2. Policies that look like fixes

allowedOrigins("*") with credentials is refused: Spring answers the first cross-origin request with 500 and sends no Allow-Origin header. The workaround allowedOriginPatterns("*") with credentials reflects any origin, so evil.example, evilbank.example and the null origin all read the account. An exact list that also lists null lets the null origin read. A pattern https://*bank.example lets evilbank.example read, because it needs no dot before bank.

### 3. An exact allow-list

With an exact list, partner.example reads /account and PUTs JSON. evil.example, evilbank.example and null are blocked, and none of them reaches the controller, because Spring answers 403 Invalid CORS request first. evil.example reads the public /rates, which carries a wildcard without credentials. The bank's own page PUTs JSON without trouble, and the Vary header on /account is Origin, so caches keep one copy for each origin.

### 4. Where the policy is registered

The exact list is registered first in Spring MVC with addCorsMappings. partner.example's preflight for PUT gets 401, because Spring Security asks for a login before Spring MVC sees the request, and a preflight never carries credentials. The simple GET works, since it needs no preflight, but the JSON PUT never reaches the bank. Registered with http.cors() the preflight is answered by a CORS filter before the login check, gets 200, and the PUT succeeds.

### 5. The bill

The public /rates sends Allow-Origin *. The browser accepts it without credentials and refuses it with credentials. With the exact list in place, evil.example's simple POST is stopped by Spring's 403, but the same POST with no Origin header, from a script on a server, gets 200 and moves the money: only that one request was processed. Every allowed origin needs a test, and so do the lookalike, the null origin and the preflight. A policy decides who may read an answer, and who may act still needs a login and a token.

## The verdict

Use an exact list of origins for anything with credentials, never reflect an origin or list null, write patterns that cannot match a lookalike, register the policy in Spring Security so the preflight is answered, send Vary: Origin, and keep logins and tokens for the callers CORS cannot see.

## How to recognise this in code you did not write

- `allowedOriginPatterns("*")` next to `allowCredentials(true)`.
- `@CrossOrigin` with no arguments.
- A preflight that returns 401 or 403.
- A wildcard on an endpoint that also takes a cookie.

## Where you have already met this

- `@CrossOrigin(origins = "*")` on a controller.
- `allowedOriginPatterns("*")` pasted from a Stack Overflow answer after an "allowCredentials" error.
- A preflight that fails with 401 while the same call works in curl.
- A browser console message: "has been blocked by CORS policy".
