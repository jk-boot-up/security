# Cross-Site Scripting with Spring Boot: Thymeleaf Escaping and a Nonce CSP, Explained

## The idea in one sentence

In Spring Boot, cross-site scripting is prevented by writing every customer value with Thymeleaf's escaping, never th:utext, and by sending a strict Content-Security-Policy whose fresh
per-request nonce is carried only by the shop's own scripts.

## The 4 acts

### 1. Thymeleaf escapes by default

The controller puts Ana's review, <b>Great boots</b>, in the model. The template product.html writes it with <p class="review" th:text="${review}">review</p>.
The server answers 200, and the page holds <p class="review">&lt;b&gt;Great boots&lt;/b&gt;</p>. A browser finds html head meta title body h1 p p a script: the shop's own elements, and no b element. The review stayed text.
Nothing had to be remembered: th:text is the ordinary way to write a value in Thymeleaf.

### 2. th:utext, the one switch that turns escaping off

The template product-unescaped.html writes the same value with th:utext, which means unescaped text. The page now holds <p class="review"><b>Great boots</b></p>, and the browser finds a b element the shop never wrote: the page changed.
th:utext is for markup the shop wrote itself, never for anything a customer typed. A search of the templates finds it in product-unescaped.html and not in product.html, and a test fails if any other template starts to use it.

### 3. The other places a value can go

Three more values go into the model: the fit note Fits 5" boots, the search size 5 & up, and the script note It's "great". The template writes them with th:attr, with a link expression @{/search(q=${search})}, and with th:inline="javascript".
Thymeleaf writes data-fit="Fits 5&quot; boots", href="/search?q=size%205%20%26%20up" and const note = "It's \"great\"";. In the attribute the quote becomes &quot;; in the link a space becomes %20 and & becomes %26, because & is character 38 = 2 x 16 + 6, which is 26 in hex; in the script the quote gets a backslash.
The browser reads the fit note back whole, finds one parameter in the link, and one string in the script with no early end.

### 4. A strict Content-Security-Policy from Spring Security

The response to GET /product carries Content-Security-Policy: default-src 'self'; script-src 'nonce-<nonce>' 'strict-dynamic'; object-src 'none'; base-uri 'none'; frame-ancestors 'none', a session cookie with HttpOnly and SameSite=Strict, and the shop's script with nonce="<nonce>".
NonceFilter makes 16 random bytes and writes them in Base64: 16 bytes make 6 groups of up to 3, and each group becomes 4 characters, so 24 characters. It runs before Spring Security writes its headers and stores the nonce as the request attribute cspNonce, which both the header writer and the template read. The header's nonce is the script's, and the next page gets a different one.
The shop's script may run; a script without the nonce is blocked.

## The verdict

Use th:text and Thymeleaf's own syntax for attributes, links and scripts. Treat every th:utext as a finding to justify, and guard them with a test. Add a nonce filter and a strict policy,
make the session cookie HttpOnly and SameSite, and keep customer text out of event handlers, styles and scheme-controlled URLs.

## How to recognise this in code you did not write

- `th:utext` in a template review.
- A `Content-Security-Policy` header with `'nonce-...'` and `'strict-dynamic'`.
- `th:inline="javascript"` blocks with `[[${...}]]` values.
- Advisories that mention CWE-79.

## Where you have already met this

- `th:text` and `th:utext` in Thymeleaf templates.
- `Content-Security-Policy` response headers with `'nonce-...'` in browser developer tools.
- `server.servlet.session.cookie.http-only` and `same-site` in Spring Boot settings.
- Code reviews that ask "why is this th:utext?"
