# CORS Done Right, Explained

## The idea in one sentence

CORS tells the browser which other sites may read an API's answers, so list those sites exactly, never reflect the Origin header or allow null, keep wildcards for public data without cookies, and use CSRF defences for forged requests.

## The 5 acts

### 1. The bank's own page needs no CORS

An origin is three parts of a web address: the scheme, the host and the port (443 when https names none). https://bank.example, the API's origin, differs from https://partner.example, https://evil.example and https://evilbank.example in the host, so a script on any of those is cross-origin.
A script on https://bank.example calls GET /account with the session cookie. It has the API's origin, so the browser applies no CORS rules and the page reads balance: 125000 cents.

### 2. With no CORS headers, a page may send but not read

A script on https://evil.example makes three requests with the customer's cookie. GET /account and POST /transfer with a text/plain body are simple requests: GET, HEAD or POST with a form or plain-text body, which the browser sends without asking. PUT with application/json is not simple.
For the PUT the browser first sends a preflight, OPTIONS /transfer with Access-Control-Request-Method: PUT and Access-Control-Request-Headers: content-type, without the cookie. The answer, 204, has no Access-Control-Allow-Origin, so the PUT is never sent.
The page reads nothing. But the bank's log shows GET /account and POST /transfer: the simple POST ran. CORS controls reading, and sending only for requests that are not simple; it is not a defence against forged requests. That is the job of the CSRF defences.

### 3. Three policies that look like fixes

Three policies that make the browser's error go away. Reflect whatever Origin arrives, with credentials: evil.example, evilbank.example and the null origin each get their own origin back, and all three read the balance.
A list with the partner that also allows null: evil.example and evilbank.example are blocked, but null is answered with null, and a sandboxed frame or a file opened locally, which an attacker can make, reads the balance.
A suffix test on bank.example: "https://evilbank.example".endsWith("bank.example") is true, so the look-alike reads the balance. Each policy works in a test with the partner's page and quietly includes the attacker's.

### 4. An exact allow-list names one partner

The fixed policy compares the Origin header as a whole string, case-sensitively, with a list that holds https://partner.example.
The partner's preflight for a JSON PUT is answered with its origin, Access-Control-Allow-Credentials: true, the methods GET, POST, PUT, the headers content-type and x-requested-with, and Max-Age 600, so the browser may cache the answer for ten minutes.
The partner's GET /account comes back with its origin, credentials true and Vary: Origin. The browser checks two things for a request with cookies: Allow-Origin equals the page's origin, and Allow-Credentials is true. Both hold, so the partner reads the balance and its PUT sends 500 to mallory.
evil.example, evilbank.example and the null origin get no CORS headers, so their reads are blocked, and the evil JSON PUT never reaches the bank. The public rates call uses *, which is fine for data read without cookies.

### 5. A wildcard and cookies never combine

Even if a server sends `*` together with `Access-Control-Allow-Credentials: true`, the browser refuses the answer for a request made with cookies, because with cookies Allow-Origin must equal the page's origin, and it accepts it for a request without cookies.
That is why teams that "need credentials and any origin" end up reflecting the Origin header, which is the unsafe policy from act three.

## The verdict

Prefer same-origin deployment or a server-to-server call. If you need CORS: an exact, case-sensitive allow-list read from configuration, `Vary: Origin`, the narrowest methods and headers, and credentials only for listed origins.
Treat CORS and CSRF as separate controls and keep both.

## How to recognise this in code you did not write

- `Access-Control-Allow-Origin` copied from the request's `Origin`.
- `Access-Control-Allow-Origin: null`.
- `endsWith`, `startsWith`, `contains` or an unanchored regex on the origin.
- A finding titled "CORS Misconfiguration" with CWE-942.

## Where you have already met this

- The browser console message "has been blocked by CORS policy".
- `Access-Control-Allow-Origin: *` on an API.
- `@CrossOrigin(origins = "*")` or a filter that copies the `Origin` header into the response.
- A finding titled "Cross-Origin Resource Sharing Misconfiguration" with CWE-942.
