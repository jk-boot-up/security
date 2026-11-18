# Cross-Site Request Forgery with Spring Security, Explained

## The idea in one sentence

Spring Security refuses a state-changing request without a token that belongs to the session, and the protection is only as strong as the switches, exemptions and GET routes left in the application.

## The 5 acts

### 1. CSRF protection switched off

The web chain has http.csrf(c -> c.disable()), the line many tutorials suggest to make a POST work.
Ana signs in: 302 and Set-Cookie: JSESSIONID=<id>; Path=/; HttpOnly, then GET /balance answers 200 balance 1000.
The request from a hidden form on https://evil.example: POST /transfer with amount=500&to=mallory, the Cookie JSESSIONID that the browser adds by itself, and Origin: https://evil.example.
The response: 200 sent 500 to mallory, a transfer Ana never asked for. The balances are now 500 and 500.

### 2. Spring Security's default token

The web chain keeps CSRF on and exempts only /webhooks/**. The sign-in form needs a token too, so that Mallory cannot sign Ana in to his own account: without one 403, with one 302.
Four requests to POST /transfer with Ana's cookie: the token from before she signed in (403, because the token is renewed at sign-in), evil.example with no token (403), evil.example with a real token from Mallory's session (403), and Ana's own form with her token (200 sent 20 to mallory).
How the token is masked: the session's token is a UUID of 36 characters. A page gets 36 random bytes followed by the token XOR those bytes, 72 bytes, written as Base64 URL text: 72 / 3 * 4 = 96 characters. Two pages differ, and the first half XOR the second half gives the same token on both.
One example byte: 0x61 ('a') XOR 0x5c = 0x3d is sent, and 0x3d XOR 0x5c = 0x61 again.
A token masked by hand with TokenMask is accepted (200 sent 20 to mallory). Logging out is a state change too: without a token 403, with one 302. The balances are 960 and 40.

### 3. A single-page app

For a single-page app the web chain uses csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()). The bank sets Set-Cookie: XSRF-TOKEN=<token>; Path=/ with HttpOnly false, so the page's script can read it, and so could a script injected by cross-site scripting.
Three requests to POST /transfer, where the browser adds both cookies: evil.example with no X-XSRF-TOKEN header (403), evil.example with the header set to Mallory's cookie value (403), and the bank's own script, which copies the cookie into the header (200 sent 20 to mallory). The balances are 980 and 20.
evil.example cannot copy the cookie, because a page reads only its own site's cookies.

### 4. What the default does not cover

Ana is signed in, and no CSRF token is sent. evil.example posts to /webhooks/payments with no signature: 401 bad signature, because the route is exempt from tokens and checks a signature instead. The payment provider, with X-Signature: 200 payment noted.
A link on evil.example, GET /legacy/move?to=mallory&amount=100: 200 sent 100 to mallory, because Spring Security never checks GET for a token. The same link to GET /transfer: 405, because the route needs POST. The balances are 900 and 100.
After one change to make a test pass, csrf.ignore=/webhooks/**,/transfer, a post from evil.example with no token gets 200 sent 500 to mallory: one line undid the defence.

### 5. The bill

The session cookie by default: Set-Cookie: JSESSIONID=<id>; Path=/; HttpOnly, so HttpOnly is true and no SameSite is set.
The API chain at POST /api/transfer keeps no session. evil.example with Ana's cookie and no Authorization header: 401 Unauthorized. Ana's app sends Authorization: Basic YW5hOmxhYi1wYXNzd29yZA==, which is Base64 of ana:lab-password: 200 sent 20 to mallory. A browser never adds a header it was not given.
Every state-changing web route, posted from evil.example with no token: /transfer 403, /logout 403, /login 403, so 3 of 3 refused.
With server.servlet.session.cookie.same-site=lax the cookie gains SameSite=Lax: one more layer, and the token is still needed.

## The verdict

Leave CSRF on for every chain that uses a session cookie, use the cookie mode with the header for single-page apps, give every exemption its own proof of origin, never change state on GET, add SameSite as a second layer, and test every state-changing route without a token.

## How to recognise this in code you did not write

- `csrf.disable()` in a copied tutorial.
- `ignoringRequestMatchers` with a broad pattern.
- An `XSRF-TOKEN` cookie in the browser's storage.
- A 403 Forbidden after a POST that worked in curl.

## Where you have already met this

- `http.csrf(csrf -> csrf.disable())` in a copied tutorial.
- `CookieCsrfTokenRepository.withHttpOnlyFalse()` in single-page app guides.
- `<input type="hidden" name="_csrf">` in Thymeleaf forms.
- A 403 Forbidden after a POST that worked in curl.
