# Dynamic Analysis, Explained

## The idea in one sentence

A scanner can only test what it reaches, so give it a login and count what it found against what you planted.

## The 5 acts

### 1. One attack, worked out by hand

The request: GET /search?q= followed by the attack string of ZAP's rule 40012, </p><scrIpt>alert(1);</scRipt><p>, sent by a small teaching class with the same string.
The response of the flawed application, status 200: <p>Results for </p><scrIpt>alert(1);</scRipt><p></p>. The attack string came back unchanged, so a browser would run it.
The response of the fixed application, status 200: the same text with every < written as &lt;, so it is shown as text. The attack string did not come back unchanged.
What ZAP reports: Cross Site Scripting (Reflected), on /search, parameter q, with the same attack; risk high, confidence medium.

### 2. The application, and a passive scan

The application under test has a search page, an account page, a login, and an admin area behind the login with its own report page. Nine flaws were planted, each with the alert a scanner should raise and the page it should raise it on: three reflected script flaws, one query built with a string, two missing cookie flags and three missing security headers.
The passive scan crawls the site and only reads what comes back; it sends no attacks.
The result: script flaws 0 of 3, query flaws 0 of 1, cookie and header flaws 5 of 5.

### 3. An active scan, without a login

The plan adds the active rules 40012 (reflected script) and 40018 (SQL injection).
How the query check works, worked out by hand with the same expression ZAP sends: id=1 gives Asha: 1200; id=3-2 gives Asha: 1200 too; id=2 gives Ben: 300. The page for 3-2 is the page for 1, so the database worked the expression out: the input reached the query as code. The fixed application answers id=3-2 with status 400.
The result: script flaws 2 of 3, query flaws 1 of 1, cookie and header flaws 5 of 5. Missed: the reflected page in the admin area. The admin area was not reached.

### 4. An active scan that is logged in

The plan adds a replacer rule: every request carries the session cookie that the login hands out.
The result: script flaws 3 of 3, query flaws 1 of 1, cookie and header flaws 5 of 5, and the admin area was reached. The total: 3 + 1 + 5 = 9 of 9 planted flaws.
The report has 10 alerts, each with a risk and a confidence: high, Cross Site Scripting (Reflected) and SQL Injection; medium, Absence of Anti-CSRF Tokens, Content Security Policy (CSP) Header Not Set and Missing Anti-clickjacking Header; low, the two cookie flags and X-Content-Type-Options Header Missing; informational, Authentication Request Identified and Session Management Response Identified.

### 5. The fixed application

The fixes: the output is escaped, the query is bound to a number, the cookie has HttpOnly and SameSite, and the security headers are sent.
Before the fixes, the logged-in scan raised 9 of 9 planted flaws: high 2, medium 3, low 3, informational 2.
After the fixes, the same scan raises 0 of 9: high 0, medium 1, low 0, informational 2.
Left for a person to judge: Absence of Anti-CSRF Tokens on /login, risk medium, confidence low. The login form has no token, and a person decides whether it matters. A clean scan says only that these probes found nothing on these pages.

## The verdict

Run a passive scan on every build, an active scan against a test copy with a login, count the scan against planted flaws, fix and rescan, and read the alerts as a list for a person.

## How to recognise this in code you did not write

- A scan that never logged in.
- A report that is green because the crawler found two pages.
- An active scan aimed at production.
- A thousand informational alerts nobody reads.

## Where you have already met this

- A yearly penetration test as the only test.
- A scan that never logged in.
- A report with hundreds of informational alerts.
- An active scan run against production.
