# Dynamic Analysis, Explained

## The idea in one sentence

A scanner can only test what it reaches, so give it a login and count what it found against what you planted.

## The 5 acts

### 1. The application under test

The site has a search page that writes the query into the page, an account page that builds its query with a string and prints errors, a login that sets a cookie without flags, an admin area that needs the session and has its own reflected page, and none of the security headers.

### 2. Passive and anonymous scans

The passive scan only reads responses: it finds the cookie flags and the missing headers and none of the script and query flaws. The active scan adds attack strings and finds the reflected script on the search and account pages and the query flaw, but the spider is sent to the login page and never reaches the admin page.

### 3. A logged-in scan

The plan sends the session cookie with every request, so the spider reaches the admin area and the active scan finds the reflected script there. All 9 of 9 planted flaws are found. The report has 2 high, 3 medium, 3 low and 2 informational alerts.

### 4. The fixed application

The fixed version escapes its output, binds the query, sets HttpOnly and SameSite on the cookie and sends the security headers. The same logged-in scan raises none of the planted flaws. One medium alert remains, no anti-CSRF token on the login form, which a person has to judge.

### 5. The bill

Without the login the admin area was never tested. A clean scan says these probes found nothing in these pages. Every alert has a risk and a confidence, and a person decides which ones are real.

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
