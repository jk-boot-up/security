# Secrets in Errors and Logs, Explained

## The idea in one sentence

Errors and logs are outputs that outlive the request, so give the customer a reference and no detail, keep secrets out of messages with types and not patterns, escape what customers type, redact as a last line of defence, and test with canaries.

## The 4 acts

### 1. What the customer sees when the database is down

The naive checkout turns the exception into the page: the exception class, its message (which holds the JDBC URL with user and password and the internal host name) and the frames of the shop's own classes. One failed payment gives a visitor the database host, the user, the password and the structure of the code.

### 2. What the log keeps

The naive logger writes the whole request, so the log has the card number, the CVV, and the session token as a JWT, and the error line has the same connection URL as the page. Anyone who can search the logs has all of it, for as long as the logs are kept.
A customer whose name is "Mallory", a line break, and "INFO login succeeded user=admin ip=10.0.0.1" produces one event in the code and two lines in the file; the second looks like a genuine login by the admin.

### 3. A generic answer, and the details where only the team looks

The fixed checkout answers with a short sentence and a reference. The log line carries the same reference, the exception's class and a redacted message, so support can find the detail and the customer sees none. The session token is a Secret and prints as [REDACTED] without any redaction pass.

### 4. Redaction, wrappers and forged lines

The redactor masks a card number only when its digits pass the Luhn check, so an order number that merely looks long is left alone. It masks values by key name (password, token, api_key, authorization, cvv and others), in text pairs and in JSON, bearer tokens, JWT-shaped strings and passwords in URLs.
The Secret wrapper prints as [REDACTED] with no pass at all. And the fixed logger turns the forged name's line break into a visible escape, so the event stays on one line and cannot be mistaken for another.

## The verdict

Return generic errors with a correlation id; log the detail with the same id. Never log request bodies, credentials, card data or tokens. Wrap secrets in a type that cannot print. Escape control characters in logged text, redact at the logging edge, set short retention, and test with canary secrets.

## How to recognise this in code you did not write

- `e.printStackTrace()` or `e.getMessage()` in a response body.
- `log.info("request: " + request)`.
- Framework debug pages enabled in production.
- Findings with CWE-209, CWE-532 or CWE-117.

## Where you have already met this

- A framework's default error page with a stack trace, in development mode.
- `log.info("request: " + request)` and `log.debug("response: " + response)` in most code bases.
- `%replace`, `MaskingConverter` and similar in logging configurations.
- A breach report that begins "credentials found in a log file".
