# Tokens in the Browser with Spring Boot: a BFF and DPoP, Explained

## The idea in one sentence

A page that holds tokens gives them to every script on it, so keep the tokens on a server, use cookies with the right attributes, and bind any token that must reach the browser to a key.

## The 5 acts

### 1. Tokens kept in the browser

The banking app signs Ana in and keeps its access token and refresh token in localStorage. The app reads her balance: 200. A third-party script on the page reads what scripts can read: 2 tokens taken. It sends them to its own server and Ana closes the page. From elsewhere, the stolen access token reads the balance: 200. Five minutes later the API refuses it as expired. The stolen refresh token gets a new access token from Keycloak, which reads the balance: one page visit, for as long as the session lives. Anything a script can read, a script can send away.

### 2. A backend for the frontend keeps the tokens on the server

The BFF signs Ana in on the server; the browser gets a session cookie __Host-bff with Path=/, Secure, HttpOnly and SameSite=Strict, and an XSRF-TOKEN cookie its script copies into a header. The cookie keeps the __Host- rules, and the BFF holds 2 tokens, an access token and a refresh token, that the browser never sees. The app reads the balance through the BFF: 200. The same third-party script reads document.cookie: only the XSRF cookie, 0 tokens taken. While the page is open, the script can still ask the BFF for the balance: 200, a script on the page can still act as Ana. Once the page is closed, what it took works nowhere else: 401. Six minutes later Ana opens the app again and the BFF refreshes the token on the server: 200, with a new access token.

### 3. Requests started by another site

A link or a hidden form on evil.example reaches the BFF, and SameSite=Strict keeps the cookie off both: the link gets 401, the form's transfer 403 with no session and no CSRF token. In an older browser that ignores SameSite, the cookie goes but the bank's request header does not: 403. A script on evil.example tries to add the header itself: the browser asks first, and the BFF names no allowed origin, so the request is never sent. The bank's own page sends the transfer with the header: 201. One transfer was made, and only the one the bank's own page asked for.

### 4. Tokens bound to a key, with DPoP

The app holds a key that scripts cannot read, and the token carries the key's thumbprint in cnf.jkt; the token type is DPoP. The app sends the token with a fresh proof signed by its key: 200. A script takes the token and sends it as a plain bearer token: 401 invalid_token. With a proof from a key of its own: 401. The app's own request copied and sent again: 401, the proof was already used. The app's proof for reading the balance, used for a transfer: 401. A proof made two minutes ago, with the token still valid: 401. A proof that does not hash the token: 401. The stolen refresh token with the thief's key is refused: the DPoP confirmation does not match the proof. With no key at all it is refused; presented by the app, with its own key, it gives new tokens.

### 5. The bill

Two BFF servers, each with its own session store: the second one answers Ana's cookie with 401, so the sessions need a shared store. Every API call now passes through one more server, which must be run, scaled and kept up to date. Clearing the browser's storage loses the DPoP key; the old token with a new key gets 401, so Ana signs in again. A script on the open page can still ask the key for a proof and read the balance: 200. DPoP stops theft, not use while the page is open. So keep scripts off the page with a strict Content-Security-Policy, and keep tokens out of it with the BFF.

## The verdict

Use a backend for the frontend for a bank's web app, give the browser a __Host- cookie and an XSRF header, refresh on the server, and where a token must reach the browser, use DPoP and keep scripts off the page with a strict Content-Security-Policy.

## How to recognise this in code you did not write

- `localStorage.setItem` with a token in front-end code.
- A `__Host-` cookie, `HttpOnly`, `SameSite=Strict`.
- `Authorization: DPoP` and a `DPoP` header.
- An `XSRF-TOKEN` cookie and an `X-XSRF-TOKEN` header.

## Where you have already met this

- `localStorage.setItem("access_token", ...)` in a tutorial.
- A cookie named `__Host-` something, `HttpOnly` and `SameSite=Strict`.
- `TokenRelay` or an API gateway that adds the access token to calls.
- `Authorization: DPoP ...` and a `DPoP:` header in a request log.
