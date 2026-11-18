# Cross-Site Scripting: Output Encoding and CSP, Explained

## The idea in one sentence

Cross-site scripting happens when untrusted text is read by the browser as part of the page; contextual output encoding keeps the text as text in every place it is written, and a strict, nonce-based Content-Security-Policy stops scripts that slip through anyway.

## The 4 acts

### 1. A review that changes the page

Ana types <b>Great boots</b> as her review. The first version of the page pastes it in as it is, so the page holds <p><b>Great boots</b></p>.
A browser finds the elements html body h1 div p b span, where plain text gives html body h1 div p span: one b element the shop never wrote. The review changed the page.
Eve types <script>steal(document.cookie)</script>. Pasted in the same way, it becomes a script element, which would run as the shop's own code: it could read the page and act as the signed-in customer.

### 2. Encoding for the place the text goes

The same rule, keep text as text, needs a different encoding in each of four places. Between tags, <b>Great boots</b> becomes &lt;b&gt;Great boots&lt;/b&gt;. In a quoted attribute, Fits 5" boots becomes Fits 5&quot; boots.
In a URL parameter, size 5 & up becomes size%205%20%26%20up. In a script string, It's "great" becomes It\u0027s \u0022great\u0022.
The table shows each risky character in each place. The URL and script codes are the character's number in hexadecimal: " is character 34, which is 2 x 16 + 2, so 22 in hex, giving %22 and \u0022.
Ben's review, encoded, reads back exactly as he typed it, and Ana's review, encoded, has no b element.

### 3. Why filtering input is not the fix

A tempting fix deletes < and > from every input. Ben's honest review Warm & dry, price < 20 & > 10 becomes Warm & dry, price 20 & 10: his words are changed.
Eve's fit note x" onclick="steal() has no angle brackets, so the filter leaves it untouched. Pasted into the quoted attribute, its quote ends data-fit early, and the browser finds a new attribute: onclick=steal(), a script that runs when the review is clicked.
Encoded for the attribute instead, the quotes become &quot;, and the browser finds only class and data-fit, whose value is Eve's exact text. A filter guesses at the place; an encoder is chosen for it.

### 4. A strict Content-Security-Policy, with a fresh nonce

The product page is served for real on this machine. The response carries Content-Security-Policy: default-src 'self'; script-src 'nonce-<nonce>' 'strict-dynamic'; object-src 'none'; base-uri 'none'; frame-ancestors 'none', and the shop's own script carries the same nonce.
The nonce is 16 random bytes, 128 bits, written in Base64, which turns every 3 bytes into 4 characters: 16 bytes make 6 groups, the last one short, so 24 characters. With bytes 00 to 0f the result would be AAECAwQFBgcICQoLDA0ODw==. The next page gets a different nonce.
The browser runs the shop's script, which carries the nonce, and blocks Eve's script, which does not. With no policy, the same script would run, and a policy with 'unsafe-inline' and no nonce protects nothing. The session cookie is HttpOnly, so no script can read it.

## The verdict

Encode on output, for the context, every time. Prefer a template engine that does it by default and review every escape hatch. Add a strict, nonce-based policy with a fresh nonce per
response, mark session cookies HttpOnly, and never treat input filtering as the fix.

## How to recognise this in code you did not write

- `th:text` against `th:utext`, or `{{ }}` against `{{{ }}}`, in templates.
- A `Content-Security-Policy` header with `'nonce-...'` and `'strict-dynamic'`.
- `innerHTML` in front-end code, the browser-side form of the same mistake.
- Advisories that mention CWE-79.

## Where you have already met this

- `&lt;` and `&amp;` in a page's source, which are encoding at work.
- `Content-Security-Policy` headers in the browser's developer tools.
- `HttpOnly` on session cookies.
- Bug-bounty reports and advisories that say "stored XSS" or "reflected XSS".
