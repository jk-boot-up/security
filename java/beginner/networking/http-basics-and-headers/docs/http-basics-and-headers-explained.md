# HTTP Basics and Security Headers, Explained

## The idea in one sentence

An HTTP message is text written by the sender, so a server must read it strictly and the same way as the proxy in front of it, keep customer text out of headers, set cookies with HttpOnly, Secure and SameSite, and send security headers whose values really restrict the browser.

## The 5 acts

### 1. An HTTP message is just text

A customer's browser sends 82 characters: a request line, headers, a blank line and a body. Parsed, it is a POST to /orders for the host shop.example with a 17-byte body. Written out again it is the same text.
Every header is only a line of text that the sender chose, so the server must check it.

### 2. Messages that two programs could read differently

A proxy and a server in a chain must agree where a request ends. Content-Length and Transfer-Encoding together are refused. Two different Content-Length headers are refused. A space before the colon in a header name is refused.
A body longer than its Content-Length, with a second request hidden after it, is refused. The same request, plain and exact, is accepted. When a proxy and a server disagree about where a request ends, the rest is read as the next customer's request.

### 3. Header injection

The store puts the customer's search text in a redirect address. A normal search, shoes, is written. Search text with a line break and a second header is refused: without the check, the attacker sets a cookie.
The answer is to refuse or encode line breaks in anything that reaches a header.

### 4. Cookies that carry a login

Four cookies are judged. session=abc123 has three problems: no HttpOnly, no Secure and no SameSite. With Secure, HttpOnly, SameSite=Lax and Path=/ it has none.
With HttpOnly and SameSite=None but no Secure it has two. And a cookie with the __Host- prefix and a Domain attribute has one, because that prefix needs Secure, Path=/ and no Domain.

### 5. Security headers on a real response

A real request to a plain server on this machine returns status 200 with security headers present: 0 of 5. The same request to a server that sets the five headers scores 5 of 5: Strict-Transport-Security, Content-Security-Policy,
X-Content-Type-Options, X-Frame-Options and Referrer-Policy. Headers present but set badly score 1 of 5, because a header is only worth its value.

## The verdict

Use a current, well-maintained server and proxy, and keep them updated. Refuse ambiguous messages and log them. Encode customer text before it reaches a header. Set session cookies with HttpOnly, Secure and SameSite=Lax, and the __Host- prefix when possible.
Send the five headers, starting HSTS with a short age. Grade the site with a scanner after every change.

## How to recognise this in code you did not write

- `Set-Cookie: ...; Secure; HttpOnly; SameSite=Lax` in login responses.
- `Strict-Transport-Security` and `Content-Security-Policy` in a framework's security configuration.
- Spring Security's `headers()` configuration and nginx's `add_header`.
- Advisories about request smuggling in proxies.

## Where you have already met this

- The browser developer tools' Network tab, which shows request and response headers.
- `curl -v`, which prints the lines of both messages.
- Scanners such as securityheaders.com and Mozilla Observatory that grade a site's headers.
- Advisories titled "HTTP request smuggling" for proxies and servers.
