# Cross-Site Request Forgery (CSRF) and Its Defences, Explained

## The idea in one sentence

A browser attaches a site's cookie to every request sent to it, so any request that changes state must also prove it came from the site's own page, with a per-session token, backed by SameSite cookies and checks of Sec-Fetch-Site and Origin, and no state change on GET.

## The 5 acts

### 1. A request Ana did not intend

Ana logs in at the bank: 200 welcome ana, and a Set-Cookie with her session id, Secure, HttpOnly and SameSite=None. Her balance is 1000.
In another tab she opens a page on evil.example whose hidden form posts to the bank. The request, as the browser sent it: POST https://bank.example/transfer,
amount=500&to=mallory, Cookie: sid=<Ana's id>, Origin: https://evil.example, Sec-Fetch-Site: cross-site.
The response: 200 sent 500 to mallory, a transfer Ana never asked for, and the balances are now 500 and 500.
The browser added the cookie because the request went to the bank; the cookie proves who is asking, not what she meant.

### 2. A token in every form

The bank's own transfer form carries a hidden field csrf with a token tied to Ana's session. The token is 32 random bytes from SecureRandom, 256 bits, written
as Base64 URL text with 6 bits a character: 256 / 6 = 42.7, so 43 characters.
Three requests, each with Ana's cookie: the bank's form with Ana's token and amount 20; a page on evil.example with no csrf field; and evil.example with a real token from Mallory's own session.
The check compares the sent token with the one in Ana's session: equal, using MessageDigest.isEqual in constant time; missing; and not equal.
The responses: 200 sent 20 to mallory, 403 no form token, and 403 the form token does not belong to this session. The balances are 980 and 20.
Another site cannot read Ana's token, because a browser does not let one site read another site's pages.

### 3. SameSite cookies

A browser finds a site from the last two labels of the host: https://evil.example is evil.example and https://bank.example is bank.example, so a request from one to the other is cross-site.
The rule for a cross-site request: None sends the cookie if it is Secure, Lax sends it only on a top-level GET link, and Strict never sends it.
With SameSite=Lax: the forged POST from evil.example goes without the cookie (401 not logged in); a link on evil.example carries it (200 balance 1000); the bank's own form carries it (200 sent 20 to mallory).
With SameSite=Strict: the link from evil.example goes without it (401, so Ana looks logged out); typing the bank's address carries it (200 balance 1000).
With no SameSite attribute, a modern browser reads it as Lax: the forged POST goes without the cookie (401).

### 4. Where the request came from, and GET

The browser picks Sec-Fetch-Site by comparing the page that started the request with the bank: https://bank.example is the same origin (same-origin), https://reviews.bank.example is the same site (same-site), and https://evil.example is another site (cross-site).
Three checked requests: the forged POST from a modern browser says Sec-Fetch-Site: cross-site and is refused with 403; the bank's own form says same-origin and works; an older browser sends no Sec-Fetch-Site, so the bank reads Origin: https://evil.example and refuses with 403.
With neither header, only the token is left, so these checks add depth and do not replace it.
A link on evil.example, GET https://bank.example/transfer?to=mallory&amount=100, carries the Lax cookie because it is a top-level GET. A bank that moves money on GET answers 200 sent 100 to mallory; a bank that needs POST answers 405 a transfer needs POST. GET must never change state.

### 5. The bill

Four requests to POST /transfer. An older browser, with SameSite=Lax as the only defence, from evil.example: 200 sent 500 to mallory, because SameSite is ignored.
SameSite=Strict as the only defence, from reviews.bank.example, where customers post content: 200 sent 500 to mallory, because it is the same site and the cookie is sent.
The same request with tokens switched on: 403 no form token. And Ana herself, from a bank page that forgot the token: 403 no form token, so the token must reach every form and client.
The bank's API, POST /api/transfer, takes a token in an Authorization header. A script on evil.example has no such header and gets 401 no API token; Ana's own app sends Authorization: Bearer <Ana's API token> and gets 200 sent 20 to mallory.
A browser never adds that header by itself, so a bearer token is safe from CSRF as long as it is not kept in a cookie.

## The verdict

Use the framework's CSRF protection and keep it on for every cookie-authenticated, state-changing request. Set session cookies to SameSite=Lax (or Strict where usability allows),
Secure and HttpOnly. Check Sec-Fetch-Site and Origin as a second layer. Never change state on GET. For APIs, prefer bearer tokens in headers, and never put them in cookies without CSRF protection.

## How to recognise this in code you did not write

- Hidden token fields named `_csrf`, `csrf_token` or `authenticity_token`.
- `SameSite=Lax` or `SameSite=Strict` in a `Set-Cookie` header.
- Servers that refuse requests with `Sec-Fetch-Site: cross-site`.
- A security finding titled "Cross-Site Request Forgery" with CWE-352.

## Where you have already met this

- Hidden `_csrf` or `authenticity_token` fields in the forms of almost every web framework.
- `Set-Cookie: ...; SameSite=Lax` on session cookies, the default in modern browsers.
- `Sec-Fetch-Site` headers in a browser's network panel.
- Breach reports where a router or bank changed settings after a visit to another page.
