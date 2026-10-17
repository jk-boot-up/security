# Cross-Site Scripting with Spring Boot: Thymeleaf Escaping and a Nonce CSP, Explained

## The idea in one sentence

In Spring Boot, cross-site scripting is prevented by writing every customer value with Thymeleaf's escaping, never th:utext, and by sending a strict Content-Security-Policy whose fresh
per-request nonce is carried only by the shop's own scripts.

## The 5 acts

### 1. Thymeleaf escapes by default

Ana's review is the text of a bold tag around "Great boots". Written with th:text, the page answers 200 and the review is written as escaped text, so the page has no bold element.
Nothing had to be remembered: th:text is the ordinary way to write a value in Thymeleaf.

### 2. th:utext, the one switch that turns escaping off

The same review written with th:utext, unescaped text, is written as markup: the page now has one bold element, and the shop wrote none. th:utext is for markup the shop wrote itself,
never for anything a customer typed. A search of the templates finds every place this can happen: one of the two, and a test fails if another one appears.

### 3. The other places a value can go

In an attribute written with th:attr, Ben's fit note with its double quote reads back whole. In a link built with @{/search(q=...)}, the search text "size 5 & up" is encoded as one
parameter, and its ampersand does not start a second one. In a script with th:inline="javascript", the note with quotes is written as one JavaScript string. Each value went through the
escaping for its own place, chosen by how it was written in the template.

### 4. A strict Content-Security-Policy from Spring Security, with a fresh nonce for every page

The product page answers with a strict policy. The policy names a nonce, and the shop's own script carries the same one. The next page has a different nonce. The shop's script may run;
a script that slipped in without the nonce may not, which is the second line of defence. The nonce comes from a filter that runs before Spring Security writes its headers, so the
header and the page always agree.

### 5. The bill

The session cookie is HttpOnly, so a script cannot read it, and SameSite=Strict. Escaping by default protects only the values written through it: th:utext and HTML built in Java code
skip it. A value placed in an event handler or a style needs more than escaping, so customer text stays out of those places. And Spring Security writes a fixed policy unless the
application supplies the nonce, as this store does with one small filter.

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
