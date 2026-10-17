# HTTP Basics and Security Headers with Spring Boot, Explained

## The idea in one sentence

A framework can write the security headers, harden the session cookie and bound the request size from a few lines of configuration, and the web server refuses a line break in a header, but the policy, the proxy trust and the limits are decisions that must be made and checked on the real responses.

## The 5 acts

### 1. The same page, with and without header writers

A customer asks for the products page over plain HTTP on this machine. The store with headers switched off scores 0 of 5. The hardened store scores 4 of 5, missing only Strict-Transport-Security. The framework leaves that header out on plain HTTP on purpose:
a browser must ignore it there, so sending it would only mislead.

### 2. The same page behind a proxy that ends HTTPS

Most stores sit behind a proxy that ends HTTPS. The proxy adds X-Forwarded-Proto: https, and the hardened store, configured to trust it, scores 5 of 5 and sends max-age=31536000 with includeSubDomains, and a policy of default-src 'self' with frame-ancestors 'none'.
This trust is safe only because the store listens on the loopback interface and only the proxy can reach it.

### 3. The cookie that carries the cart's session

The cart starts a session and sets a cookie. In the default store the cookie JSESSIONID has two problems: no Secure, and no SameSite. HttpOnly is already on by default. In the hardened store three properties, cookie.http-only, cookie.secure and cookie.same-site,
leave the cookie with no problems.

### 4. A line break inside a header

The search endpoint puts the customer's text in the X-Echo header. A text with a line break and a second header is refused: the container will not write it, the status is 500, and the answer has no second header with the attacker's cookie and no X-Echo header at all.
A request with a space in a header name gets status 400. The container turns the attack into an error, and the code must still never write customer text into a header unchecked.

### 5. Limits on the size of a request

A request with a 4000-character header is answered 200 by the default store, and 400 by the hardened store whose header limit is one kilobyte. A small limit keeps one customer from making the server read and hold megabytes, and the limit must still be high enough
for real cookies.

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
