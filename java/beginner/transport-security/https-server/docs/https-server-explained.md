# An HTTPS Server, Put Together, Explained

## The idea in one sentence

A secure web server needs a trusted certificate, a modern TLS version, a redirect from plain HTTP, HSTS, and cookies that cannot leave over HTTP, and clients that check all of it and refuse an impostor, a wrong name or a downgrade.

## The 5 acts

### 1. An HTTPS request that is checked

The client sends a request to the shop over HTTPS. It checks that the certificate is one it trusts, that it names the shop, and that it is current. The shop answers 200 over TLS 1.3, with five of the five security headers including HSTS.

### 2. Plain HTTP only redirects

A request to the plain port gets a 301, moved permanently, pointing to an https address. The plain answer carries no HSTS header, because browsers ignore HSTS sent over plain HTTP.
The first request itself crossed the network in the clear. HSTS protects the later visits, not the first.

### 3. A cookie that stays on HTTPS

The login sets a cookie marked Secure, HttpOnly and SameSite=Lax. Asked again over HTTPS, the cookie is sent. Asked over plain HTTP, it is not sent: the Secure flag keeps it off plain connections.
A cookie without the Secure flag, asked over plain HTTP, is sent, and that is a leak.

### 4. What the client refuses

A server with the right name and a certificate nobody trusts is refused. The right certificate reached under another name is refused. A client that demands TLS 1.3 refuses a server that offers only 1.2. The same server works for a client that also allows 1.2, over TLS 1.2.
Allowing the older version is a choice the client makes, and it is what a downgrade attack asks for.

### 5. Scoring a shop from outside

An auditor's checklist probes a running shop from outside: the certificate is trusted and matches, TLS 1.3 is accepted, plain HTTP redirects, HSTS lasts a year, and the session cookie is Secure, HttpOnly and SameSite.
The careful shop scores 5 of 5. The careless shop has the same valid certificate and scores 1 of 5. A padlock proves who the server is and that the connection is private; it says nothing about the rest.

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
