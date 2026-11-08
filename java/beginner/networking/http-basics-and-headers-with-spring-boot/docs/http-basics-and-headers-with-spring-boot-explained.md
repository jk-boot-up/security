# HTTP Basics and Security Headers with Spring Boot, Explained

## The idea in one sentence

A framework can write the security headers, harden the session cookie and bound the request size from a few lines of configuration, and the web server refuses a line break in a header, but the policy, the proxy trust and the limits are decisions that must be made and checked on the real responses.

## The 5 acts

### 1. The same page, with and without header writers

A customer asks both stores for GET /products over plain HTTP on this machine. Both answer status 200. The bare store, whose filter chain switches the headers off, sends none of the five security headers, so the audit adds 0 + 0 + 0 + 0 + 0 = 0 of 5.
The hardened store sends Content-Security-Policy default-src 'self'; frame-ancestors 'none', X-Content-Type-Options nosniff, X-Frame-Options DENY and Referrer-Policy strict-origin-when-cross-origin, so it scores 0 + 1 + 1 + 1 + 1 = 4 of 5.
The missing one is Strict-Transport-Security: the framework leaves it out on plain HTTP on purpose, because a browser must ignore it there, so sending it would only mislead.

### 2. The same page behind a proxy that ends HTTPS

Most stores sit behind a proxy that ends HTTPS: the customer's browser speaks HTTPS to the proxy, and the proxy passes the request on over plain HTTP, adding X-Forwarded-Proto: https. The hardened store is set to trust it (server.forward-headers-strategy=native),
so it treats the request as secure and adds Strict-Transport-Security: max-age=31536000 ; includeSubDomains. The max-age is in seconds: 31536000 / 86400 seconds a day = 365 days, one year. Now the audit adds 1 + 1 + 1 + 1 + 1 = 5 of 5.
This trust is safe only because the store listens on the loopback interface and only the proxy can reach it: otherwise a customer could send the header and change what the store believes.

### 3. The cookie that carries the cart's session

The cart starts a session, and Tomcat sends the cookie JSESSIONID with a random 32-character id. In the default store the header is JSESSIONID=...; Path=/; HttpOnly: HttpOnly is on by default, but it has two problems, no Secure (it is also sent over plain HTTP) and no SameSite (other sites can trigger requests that carry it).
In the hardened store three properties, server.servlet.session.cookie.http-only=true, .secure=true and .same-site=strict, make it JSESSIONID=...; Path=/; Secure; HttpOnly; SameSite=Strict, with no problems.

### 4. A line break inside a header

The search endpoint puts the customer's text in the X-Echo header. The normal text shoes comes back as X-Echo: shoes with status 200. The attack text shoes\r\nSet-Cookie: evil=1 hides a line break and a second header. Tomcat refuses to write a header value with a line break: the status is 500,
and the answer has no X-Echo header and no Set-Cookie header with the attacker's cookie. A request sent as raw bytes with a space in a header name, Bad Header: x, gets status 400. The container turns both into errors, and the code must still never write customer text into a header unchecked.

### 5. Limits on the size of a request

A request carries one header made of 4000 letters a: X-Padding: (11 characters), the 4000 letters and \r\n (2), so 11 + 4000 + 2 = 4013 bytes, and 4078 bytes for the whole request. The default store's limit on the request's headers is Spring Boot's 8KB = 8192 bytes, so it answers 200.
The hardened store sets server.max-http-request-header-size=1KB = 1024 bytes, so it answers 400. A small limit keeps one customer from making the server read and hold megabytes, and the limit must still be high enough for real cookies.

## The verdict

Use the framework's header support, set the cookie properties, set request limits from real traffic, trust forwarded headers only from the proxy, and test the real responses after every upgrade, as the demo does.

## How to recognise this in code you did not write

- `http.headers(...)` and `SecurityFilterChain` in Spring configuration.
- `server.servlet.session.cookie.*` in `application.properties`.
- Header grading tools for websites.

## Where you have already met this

- `http.headers(...)` in a Spring Security configuration class.
- `server.servlet.session.cookie.*` properties in `application.properties`.
- Security scanners that give a site a grade for its headers.
- `server.max-http-request-header-size` and its relatives in hardening guides.
