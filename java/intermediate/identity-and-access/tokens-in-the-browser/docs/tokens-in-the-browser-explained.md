# Tokens in the Browser, Explained

## The idea in one sentence

Tokens kept in a browser are readable by every script on the page, so a backend for the frontend keeps them on the server and gives the browser only an HttpOnly
cookie, and DPoP binds any token that must be used from the client to a key that never leaves it.

## The 5 acts

### 1. Tokens kept in the browser

The banking app signs Ana in and writes both tokens to localStorage. The access token is a signed JWT; its middle part, decoded, says sub ana, scope accounts.read payments.write,
iat 1790000000 and exp 1790000300, which is iat plus 300 seconds. The refresh token is 32 random bytes, valid 30 days. The app reads the balance with the access token: 200.
A third-party script on the page reads localStorage and sends both tokens to its own server, and Ana closes the page. From elsewhere the stolen access token reads the balance:
200. 300 seconds later it has expired: 401. Ten days later the stolen refresh token gets a new access token, which reads the balance: 200. One page visit gave a month of access.

### 2. A backend for the frontend keeps the tokens on the server

The BFF signs Ana in on the server; the browser gets one cookie, __Host-bff, whose value is a random session id, with Path=/, Secure, HttpOnly and SameSite=Strict. Secure means
it is sent over HTTPS only, HttpOnly that scripts on the page cannot read it, SameSite=Strict that it is not sent when another site starts the request, and the __Host- prefix
that it must be Secure, have Path=/ and no Domain. The BFF's session store holds 2 tokens; the browser holds 0 tokens and 1 cookie. The app reads the balance through the BFF:
200. The same third-party script takes 0 tokens. While the page is open it can still ask the BFF for the balance: 200. Once the page is closed, the script is no longer
running. Six minutes later, after the access token has expired, the browser sends only the cookie and the BFF refreshes the token on the server: 200.

### 3. Requests started by another site

A cookie that is sent automatically invites cross-site request forgery. A hidden form on evil.example posts a transfer to the BFF without the X-Bank-Request header, which a form
cannot add. In a modern browser SameSite=Strict keeps the cookie off it: 401. In an older browser that ignores SameSite, the cookie goes, but the header does not: 403. A script on
evil.example that adds the header itself must first ask in a CORS preflight, which the BFF refuses for every other origin, so the request is never sent. The bank's own page
sends the transfer with the cookie and the header: 201. Transfers made: 1.

### 4. Tokens bound to a key, with DPoP

When tokens must be in the browser, DPoP binds them to a key that scripts can use but cannot read. The key's public half, as a JWK, is written with its members crv, kty and x in
alphabetical order and no spaces, 79 characters; SHA-256 of that text, in base64url, is the thumbprint, and the access token carries it in cnf.jkt. Each request carries a proof:
a small JWT signed by the key, with a random jti, the method (htm), the URL (htu), the time (iat) and ath, the SHA-256 of the access token. With a fresh proof: 200. The token
alone: 401. With a proof from the thief's key: 401. The app's request sent again: 401, the proof was already used. A proof for GET /accounts used for a transfer: 401. A proof
made 120 seconds ago: 401, too old. The refresh token is bound to the same key: refused with the thief's key, new tokens with the app's.

### 5. The bill

Two BFF servers, each with its own session store: Ana signs in on the first, her cookie reaches the second, which answers 401, so the sessions need a shared store. Every API call
now passes through one more server, which must be run, scaled and kept up to date. Clearing the browser's storage loses the DPoP key: the token is bound to one thumbprint, the new
key has another, so the bank answers 401 and Ana signs in again. And a script on the open page can still ask the key for a proof and read the balance: 200. DPoP stops theft, not use
while the page is open. So keep scripts off the page with a strict Content-Security-Policy, and keep tokens out of it with the BFF.

## The verdict

For browser apps that hold OAuth tokens, use a backend for the frontend: a confidential client on the server, a __Host- cookie with Secure, HttpOnly and
SameSite=Strict, and a request header on state-changing calls. Where tokens must be used from the client, bind them with DPoP. In both cases, keep third-party
scripts to a minimum and enforce a strict Content-Security-Policy, because code on the open page can still act as the user.

## How to recognise this in code you did not write

- `localStorage.setItem("access_token", ...)` in a single-page app.
- A session cookie named `__Host-...` on a banking app, and no token in the browser's storage.
- `Authorization: DPoP` and `DPoP:` headers, and `cnf` with `jkt` in an access token.
- Library settings called "token handler", "BFF" or "sender-constrained tokens".

## Where you have already met this

- Banking and health apps that sign in through a page on their own domain and never show a token.
- `Authorization: DPoP ...` and a `DPoP:` header in API requests.
- Security advisories about a compromised analytics or chat script on a popular site.
- Frameworks that ship a "BFF" or "token handler" component for single-page apps.
