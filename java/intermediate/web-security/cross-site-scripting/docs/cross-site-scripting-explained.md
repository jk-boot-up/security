# Cross-Site Scripting: Output Encoding and CSP, Explained

## The idea in one sentence

Cross-site scripting happens when untrusted text is read by the browser as part of the page; contextual output encoding keeps the text as text in every place it is written, and a strict, nonce-based Content-Security-Policy stops scripts that slip through anyway.

## The 5 acts

### 1. A review that changes the page

Ana's review is the text <b>Great boots</b>, pasted into the page as it is. The page now has a bold element, and the shop wrote none: the review became part of the page. The browser cannot tell the shop's markup from the customer's,
so a script in a review would run as the shop's own: it could read the page, act as the signed-in customer, and send what it finds elsewhere.

### 2. Encoding for the place the text goes

Between tags, <b>Great boots</b> becomes &lt;b&gt;Great boots&lt;/b&gt;. In a quoted attribute, Fits 5" boots becomes Fits 5&quot; boots. In a URL parameter, size 5 & up becomes size%205%20%26%20up. In a script string, the quotes become \u0027 and \u0022.
The encoded page has no bold element, and Ben's fit note reads back whole. Every value shows as the text the customer typed, and none of it is read as markup.

### 3. Why filtering input is not the fix

An honest review says Warm & dry, price < 20 & > 10. A filter that deletes < and > turns it into Warm & dry, price 20 & 10: the customer's words are changed. Encoded instead, the page shows it exactly. The same filter keeps the quote in Ben's fit note,
so the attribute ends early and reads only Fits 5: the filter did not know the context. A filter guesses at every place text will go; an encoder is chosen for the one place it does go.

### 4. A strict Content-Security-Policy, with a fresh nonce for every page

The product page, served for real on this machine, answers 200 with a policy. The policy names a nonce, and the shop's own script carries it. The nonce is 24 characters of random text, and the next page has a different one. The shop's script may run;
a script that slipped in without the nonce may not, which is a second line of defence. With no policy, the same script would run, and a policy with 'unsafe-inline' and no nonce protects nothing. The session cookie is HttpOnly, so a script cannot read it.

### 5. The bill

This one page writes untrusted text into 4 kinds of place, and each needs its own encoder; one forgotten place is enough. The policy needs care: every inline script must carry the nonce, and every new source must be added on purpose. The nonce must be new for
every response, or a page from yesterday hands an attacker today's key. In practice, a template engine that encodes by default does the first part: see the tools version of this project.

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
