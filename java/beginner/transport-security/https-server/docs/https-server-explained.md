# An HTTPS Server, Put Together, Explained

## The idea in one sentence

A secure web server needs a trusted certificate, a modern TLS version, a redirect from plain HTTP, HSTS, and cookies that cannot leave over HTTP, and clients that check all of it and refuse an impostor, a wrong name or a downgrade.

## The 5 acts

### 1. An HTTPS request that is checked

The customer's client asks for https://shop.example.test/cart. Before any data moves, the shop presents its certificate: subject CN=shop.example.test, issued by itself, naming shop.example.test, valid from 2026-10-17 to 2126-09-23.
The client makes three checks: it is the one certificate the client trusts, it names the host that was asked for (shop.example.test), and today is between the two dates. All three pass, so the request is answered 200 cart: 2 items over TLSv1.3,
with the five security headers: 1 + 1 + 1 + 1 + 1 = 5 of 5.

### 2. Plain HTTP only redirects

A customer types the old address, http://shop.example.test/cart. The plain port does not serve the cart: it answers 301 Moved Permanently with Location: https://shop.example.test:<https port>/cart, and no Strict-Transport-Security header, because browsers ignore HSTS sent over plain HTTP.
The HTTPS answer carries Strict-Transport-Security: max-age=31536000; includeSubDomains, and 31536000 / 86400 seconds a day = 365 days in which the browser uses HTTPS only. The first request still crossed the network in the clear: HSTS protects only the visits after it.

### 3. A cookie that stays on HTTPS

The login over HTTPS sets session=...; Secure; HttpOnly; SameSite=Lax; Path=/. On the next requests the browser attaches it over https (the shop says cookie received) and does not attach it over http, because of Secure.
Before the fix, a shop whose cookie is just session=...; Path=/ has the cookie sent over plain http too: a leak, since anyone on the network can read it there and use the session.

### 4. What the client refuses

Four attempts. A server at shop.example.test with a certificate nobody trusts: refused (SSLHandshakeException), not trusted. The shop's own certificate reached as evil.example.test: refused, wrong name. A client that allows only TLS 1.3, against a server that offers only TLS 1.2: refused, no common version.
The same server with a client that also allows 1.2: ok (TLSv1.2). Allowing the older version is the client's choice, and it is exactly what a downgrade attack asks for.

### 5. Scoring a shop from outside

Two shops are checked from outside on the same five points: the certificate is trusted and names the host, TLS 1.3 is accepted, plain HTTP redirects to HTTPS, HSTS lasts at least a year, and the session cookie is Secure, HttpOnly and SameSite.
The careful shop passes all five, 1 + 1 + 1 + 1 + 1 = 5 of 5. The careless shop has the same valid certificate but passes nothing else, 1 + 0 + 0 + 0 + 0 = 1 of 5. A padlock proves who the server is and that the connection is private; it says nothing about the rest of the setup.

## The verdict

Treat HTTPS as a set of settings, not a certificate. Redirect plain HTTP, send HSTS once everything works, mark cookies Secure, HttpOnly and SameSite, offer TLS 1.3, renew certificates automatically, and test the result from outside.

## How to recognise this in code you did not write

- `Strict-Transport-Security` and `Set-Cookie: ...; Secure; HttpOnly; SameSite=...` in a browser's developer tools.
- A 301 from `http://` to `https://` in a `curl -I` call.
- SSL Labs and other scanners that give a grade to a server.
- Automatic certificate renewal with ACME clients.

## Where you have already met this

- The padlock in the browser's address bar, and the warning page when a certificate is wrong.
- Sites that jump from `http://` to `https://` as soon as they load.
- The `Strict-Transport-Security` and `Set-Cookie` lines in browser developer tools.
- SSL Labs and similar tests that score a server's configuration.
