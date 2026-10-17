# Cross-Site Request Forgery (CSRF) and Its Defences, Explained

## The idea in one sentence

A browser attaches a site's cookie to every request sent to it, so any request that changes state must also prove it came from the site's own page, with a per-session token, backed by SameSite cookies and checks of Sec-Fetch-Site and Origin, and no state change on GET.

## The 5 acts

### 1. A request Ana did not intend

Ana logs in at the bank and her browser keeps the session cookie; her balance is 1000. In another tab she opens a page on evil.example, whose hidden
form posts to the bank: send 500 to mallory. The bank answers 200 and sends it: a transfer Ana never asked for. The balances are now 500 and 500.
The browser added Ana's cookie because the request went to the bank; the cookie proves who is asking, not what she meant.

### 2. A token in every form

The bank's own transfer form carries a token tied to Ana's session, and Ana's transfer of 20 with that form works. The page on evil.example posts the
same form without a token and is refused: 403, no form token. With a real token from Mallory's own session it is refused too: the token does not
belong to this session. Another site cannot read Ana's token, because the form lives on the bank's pages and a browser does not let another site read them.
The token is compared in constant time, so the time an answer takes does not reveal how much of a guess was right.

### 3. SameSite cookies

With SameSite=Lax the forged post from evil.example arrives without the cookie and the bank answers 401. A link on evil.example to the bank still
opens Ana's account, because Lax allows cookies on top-level links, and the bank's own form still works. With SameSite=Strict even that link arrives without the
cookie, so Ana looks logged out, while typing the bank's address works. With no SameSite attribute at all, a modern browser treats the cookie as Lax.

### 4. Where the request came from, and GET

A modern browser says Sec-Fetch-Site: cross-site for the forged post, and the bank refuses it; the bank's own form says same-origin and works. An older browser
sends no Sec-Fetch-Site, so the bank reads Origin, https://evil.example, and refuses. With neither header only the token is left, so these checks add depth and do not
replace it. A bank that moves money on GET loses 100 to a plain link on evil.example even with a Lax cookie, because Lax lets cookies ride on top-level links; the same link
to a bank that needs POST is refused with 405. GET must never change state.

### 5. The bill

With SameSite as the only defence, an older browser that ignores it sends the forged transfer. With SameSite=Strict, a page on reviews.bank.example, where customers post
content, is the same site, so the cookie is sent and the transfer goes through. The same page with tokens switched on is refused. But a bank page that forgot to include
the token refuses Ana herself, so the token must reach every form and client. Finally, the bank's API takes a token in an Authorization header: a script on evil.example
cannot add a header it does not know, so it is refused with 401, while Ana's own app, which holds the token, works. APIs with bearer tokens are not exposed in the same way,
as long as the token is not kept in a cookie.

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
