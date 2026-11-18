# CORS with Spring Boot, Explained

## The idea in one sentence

CORS decides which pages may read an answer from the browser, and Spring Boot enforces the list you give it, so the list must be exact and registered before the login check.

## The 5 acts

### 1. With no policy, a page may send but not read

A script on https://evil.example makes three requests with the customer's login. GET /account and POST /transfer with a text/plain body are simple requests, which the browser sends without asking. PUT with application/json is not simple, so the browser first sends a preflight.
The preflight, OPTIONS /transfer, carries no login, so Spring Security answers 401 with no Access-Control-Allow-Origin, and the PUT is never sent. The page reads nothing, but the bank's log shows GET /account and POST /transfer: CORS did not stop the POST. The bank's own page, on the same origin, reads the balance.

### 2. Four policies that look like fixes

allowedOrigins [*] with allowCredentials true is refused by Spring: the first cross-origin request gets 500 and no Access-Control-Allow-Origin. The usual workaround, allowedOriginPatterns [*], sends each origin back, so evil.example, evilbank.example and the null origin all get 200 and read the balance.
A list of https://partner.example and null: Spring answers evil.example and evilbank.example with 403, but sends null back to the null origin, which a sandboxed frame has, so it reads the balance.
The pattern https://*bank.example: the * may be any text, so https://evilbank.example matches (CorsConfiguration.checkOrigin says so), while https://partner.example and https://evil.example do not. The look-alike reads the balance.

### 3. An exact allow-list names one partner

CorsSettings.accounts lists one origin, https://partner.example, the methods GET, POST and PUT, the header Content-Type, allowCredentials true and maxAge 600. The partner's preflight for a JSON PUT gets 200 with exactly those values.
The partner reads the balance and its PUT sends 500 to mallory. evil.example, evilbank.example and the null origin never reach the bank, nor does evil.example's JSON PUT. The public /rates is readable without credentials, and the bank's own page needs no policy.
Spring's own answer to evil.example is 403 Invalid CORS request, and the bank's log grew by 0: the controller never ran. Answers carry Vary: Origin, Access-Control-Request-Method and Access-Control-Request-Headers, so a cache keeps one copy for each.

### 4. The policy must sit before the login check

A request goes through Spring Security first, where http.cors runs before the login check, and then Spring MVC, where addCorsMappings runs before the controller.
Registered in Spring MVC, the same exact list never sees the preflight: a preflight carries no login, so the login check answers 401. The simple GET works, since it needs no preflight, but the JSON PUT never reaches the bank.
Registered with http.cors, the preflight is answered by the CORS filter before the login check: 200, and the PUT sends 500 to mallory.

### 5. CORS is a browser rule, not a defence

The public /rates sends Access-Control-Allow-Origin: *. The browser accepts it for a request without credentials and refuses it for one with credentials.
With the exact list in place, a simple POST /transfer from a page on https://evil.example never reaches the bank. The same POST from a script on a server, which sends no Origin header and is not a browser, gets 200 sent 500 to mallory, and the bank's log shows [POST /transfer].
CORS decides who may read an answer inside a browser. Who may act still needs a login and a CSRF token.

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
