# Tokens in the Browser, Explained

## The idea in one sentence

Tokens kept in a browser are readable by every script on the page, so a backend for the frontend keeps them on the server and gives the browser only an HttpOnly
cookie, and DPoP binds any token that must be used from the client to a key that never leaves it.

## The 5 acts

### 1. Tokens kept in the browser

The banking app signs Ana in and keeps its access token and refresh token in localStorage, and reads her balance: 200. A third-party script on the page
reads what scripts can read and takes 2 tokens, sends them to its own server, and Ana closes the page. From elsewhere, the stolen access token reads the
balance: 200. Five minutes later it has expired: 401. But ten days later, the stolen refresh token gets a new access token, which reads the balance: 200.
One page visit gave a month of access. Anything a script can read, a script can send away.

### 2. A backend for the frontend keeps the tokens on the server

The BFF signs Ana in on the server; the browser gets one cookie, __Host-bff, with Path=/, Secure, HttpOnly and SameSite=Strict, which keeps the __Host-
rules. The BFF holds the 2 tokens and the browser never sees them. The app reads the balance through the BFF: 200. The same third-party script now takes
0 tokens, and cannot read the cookie. While the page is open it can still ask the BFF for the balance: 200, because a script on the page can still act as
Ana. Once the page is closed, the script is no longer running, and nothing it took works anywhere else. Six minutes later the BFF refreshes the token on the
server without the browser noticing: 200.

### 3. Requests started by another site

A cookie that is sent automatically invites cross-site request forgery. A hidden form on evil.example posts a transfer to the BFF, and SameSite=Strict keeps
the cookie off it: 401. In an older browser that ignores SameSite, the cookie goes, but the bank's request header does not: 403. A script on evil.example
that tries to add the header itself must first ask in a CORS preflight, which the BFF refuses for every other origin, so the request is never sent. The
bank's own page sends the transfer with the header: 201. Transfers made: 1, the one the bank's own page asked for.

### 4. Tokens bound to a key, with DPoP

When tokens must be in the browser, DPoP binds them to a key that scripts cannot read: the access token carries the key's thumbprint in cnf.jkt, and each
request carries a proof, a small JWT signed by that key, naming the method, the URL, the time, a unique id and the token's hash. With a fresh proof: 200.
A script that takes the token from localStorage and sends it as a plain bearer token: 401. With a proof from a key of its own: 401, the key does not match.
The app's own request copied and sent again: 401, the proof was already used. The proof for reading the balance used for a transfer: 401. A proof made two
minutes ago, with the token still valid: 401, too old. The stolen refresh token with the thief's key is refused; presented by the app with its own key, it
gives new tokens. A stolen token is useless without the key, and the key never leaves the browser.

### 5. The bill

Two BFF servers, each with its own session store: the second answers Ana's cookie with 401, so the sessions need a shared store. Every API call now passes
through one more server, which must be run, scaled and kept up to date. Clearing the browser's storage loses the DPoP key; the old token with a new key is
refused, so Ana signs in again. And a script on the open page can still ask the key for a proof and read the balance: 200. DPoP stops theft, not use while
the page is open. So keep scripts off the page with a strict Content-Security-Policy, and keep tokens out of it with the BFF.

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
