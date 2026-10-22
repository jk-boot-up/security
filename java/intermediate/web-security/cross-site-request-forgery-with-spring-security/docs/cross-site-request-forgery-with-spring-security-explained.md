# Cross-Site Request Forgery with Spring Security, Explained

## The idea in one sentence

Spring Security refuses a state-changing request without a token that belongs to the session, and the protection is only as strong as the switches, exemptions and GET routes left in the application.

## The 5 acts

### 1. CSRF protection switched off

A tutorial's advice to make a POST work is to disable CSRF protection. With it off, Ana signs in and her browser keeps the session cookie. A page on evil.example posts a transfer to the bank, and the browser adds the cookie because the request goes to the bank. The bank answers 200 and sends 500 to Mallory.

### 2. Spring Security's default token

By default every post needs a token tied to the session. Without one, even the sign-in form is refused with 403, and the token is renewed when Ana signs in, so one taken before that no longer works. A forged post without a token gets 403, and so does one with a token from Mallory's own
session. Ana's form with her own token works. Each page gets a different masked token, all accepted, which defends against BREACH. Logout is a state change, so it needs a token too.

### 3. A single-page app

A single-page app cannot render a hidden field, so the token is in an XSRF-TOKEN cookie that the page's script copies into an X-XSRF-TOKEN header. The forged post has no header and gets 403, and so does one with the value from Mallory's own cookie. The bank's own script works. The cookie cannot be
HttpOnly because the script must read it, so a script injected by cross-site scripting can read it as well.

### 4. What the default does not cover

The payment provider's webhook has no session, so it is exempt from tokens, and a forged post without its signature gets 401: the route checks its own proof. A route that moves money on GET is forged by a link on evil.example, because Spring never checks GET for a token; the same transfer
on a POST-only route gets 405. And a developer who adds /transfer to the exemptions to make a test pass lets a forged post through: one line of configuration undid the defence.

### 5. The bill

The session cookie is HttpOnly by default but has no SameSite attribute; a property adds SameSite=Lax as one more layer. The API chain takes credentials in a header and keeps no session, so Ana's cookie from evil.example gets 401, and the app that holds the credentials gets 200: that chain can leave CSRF
off, because a browser never adds a header it was not given. A final check posts to every state-changing route of the web chain with no token, and all 3 refuse with 403.

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
