# HTTP Basics and Security Headers, Explained

## The idea in one sentence

An HTTP message is text written by the sender, so a server must read it strictly and the same way as the proxy in front of it, keep customer text out of headers, set cookies with HttpOnly, Secure and SameSite, and send security headers whose values really restrict the browser.

## The 5 acts

### 1. An HTTP message is just text

A customer's browser sends five lines: the request line POST /orders HTTP/1.1, the headers host and content-length, a blank line and the body item=A-1001&qty=2. Each line ends with \r\n, two invisible
characters, so the lines count 23 + 20 + 20 + 2 + 17 = 82 characters. Parsed, it is a POST to /orders for the host shop.example with a 17-byte body, exactly what Content-Length said.
Written out again it is the same text. Every header is only a line of text that the sender chose, so the server must check it.

### 2. Messages that two programs could read differently

A proxy and a server in a chain must agree where a request ends. A request says Content-Length: 4, but after the four body bytes abcd comes a whole second request: GET /admin HTTP/1.1\r\n (21 bytes),
Host: shop.example\r\n (20) and a blank line (2), so 4 + 21 + 20 + 2 = 47 bytes follow the blank line. A proxy that trusts Content-Length stops after 4 bytes and reads the other 43 as the next customer's request,
which is the idea behind request smuggling. The strict parser refuses it: Content-Length says 4 bytes but 47 follow.
It also refuses Content-Length and Transfer-Encoding together, the same with TRANSFER-ENCODING in capitals on a machine set to Turkish (where the capital I lower-cases to a dotless ı, so header names are
lower-cased without the machine's locale), two different Content-Length headers, and a space before the colon. The same request, plain and exact, is accepted.

### 3. Header injection

The store puts the customer's search text in the Location header of a redirect. A normal search, shoes, is written as Location: /search?q=shoes. The attack text is shoes\r\nSet-Cookie: session=attacker.
Before the fix, written as it is, the browser reads three lines: the status line, Location: /search?q=shoes and Set-Cookie: session=attacker, a header the attacker wrote. After the fix, the writer refuses a line break
inside any header value, so the attack is refused. The answer is to refuse or encode line breaks in anything that reaches a header.

### 4. Cookies that carry a login

Four cookies are judged. session=abc123 has three problems: no HttpOnly, no Secure and no SameSite. With Secure, HttpOnly, SameSite=Lax and Path=/ it has none.
With HttpOnly and SameSite=None but no Secure it has two. And a cookie with the __Host- prefix and a Domain attribute has one, because that prefix needs Secure, Path=/ and no Domain.

### 5. Security headers on a real response

A real request to a plain server on this machine returns status 200 and none of the five security headers: 0 of 5. The same request to a server that sets the five headers scores 5 of 5: Strict-Transport-Security,
Content-Security-Policy, X-Content-Type-Options, X-Frame-Options and Referrer-Policy. Headers present but set badly (max-age=300, script-src 'unsafe-inline', nosniff, ALLOWALL, unsafe-url) score
0 + 0 + 1 + 0 + 0 = 1 of 5, because a header is only worth its value.

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
