# CORS Done Right, Explained

## The idea in one sentence

CORS tells the browser which other sites may read an API's answers, so list those sites exactly, never reflect the Origin header or allow null, keep wildcards for public data without cookies, and use CSRF defences for forged requests.

## The 5 acts

### 1. Same origin needs no CORS

A script on https://bank.example calls the bank's API on the same origin and reads the balance. The browser applies no CORS rules at all.

### 2. No CORS headers: a page may send, but not read

With no CORS headers, a script on evil.example reads nothing: "answer blocked". But the request reached the bank, with the customer's cookie, and a simple POST with a text/plain body is processed: the bank's log has POST /transfer.
A JSON PUT needs a preflight, which fails, so that one never reaches the bank. CORS controls reading and, for non-simple requests, sending; it is not a defence against forged simple requests. That is the job of the CSRF defences.

### 3. Policies that look like fixes

Three policies that make the console error go away: reflect whatever Origin arrives, with credentials (evil.example, evilbank.example and a null origin all read the balance); an exact list that also allows `null` (a sandboxed iframe or a
file opened locally reads it); and a suffix match on `bank.example`, which accepts https://evilbank.example. Each is a configuration that works in a test with the partner's page and quietly includes the attacker's.

### 4. An exact allow-list

The fixed policy compares the Origin header as a whole, case-sensitively, with a list that has one partner. partner.example reads the account and sends a JSON PUT after the preflight approves the method and header.
evil.example, evilbank.example and the null origin all get no CORS headers, so their reads are blocked, and the evil JSON PUT never reaches the bank. The public rates call uses a wildcard, which is fine for data with no cookies.
Responses carry `Vary: Origin` so that a cache does not hand one origin's answer to another.

### 5. Wildcard and cookies never combine

Even if a server sends `*` together with `Access-Control-Allow-Credentials: true`, the browser refuses the answer for a request made with cookies and accepts it for one without. That is why teams that "need credentials and any origin"
end up reflecting the Origin header, which is the unsafe policy from act three.

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
