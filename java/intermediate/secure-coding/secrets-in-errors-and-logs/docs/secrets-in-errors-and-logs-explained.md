# Secrets in Errors and Logs, Explained

## The idea in one sentence

Errors and logs are outputs that outlive the request, so give the customer a reference and no detail, keep secrets out of messages with types and not patterns, escape what customers type, redact as a last line of defence, and test with canaries.

## The 4 acts

### 1. What the customer sees when the database is down

The input is one checkout request: name Ana, order 1042, the test card 4111 1111 1111 1111, CVV 737 and a session token that is a JWT.
The database is down, and the driver's SQLException names the JDBC URL. The naive checkout turns the exception into the page: its class, its message and the frames of the shop's own classes.
The result, read the way a visitor would read it (PageReader): host db.internal.shop.example, port 5432, database shop, user app, password s3cr3t-db-password, and the code path Database.saveOrder, Checkout.pay.

### 2. What the log keeps

The naive logger writes the whole request map and the token, so line 1 holds card=4111 1111 1111 1111, cvv=737 and the JWT. Line 2 is the error with the same JDBC URL and password as the page.
A canary search looks for each known fake secret: card number, CVV and session token are in the log only; the database password is in the page and the log.
The second input is a name with a line break: "Mallory", a line break, and "INFO login succeeded user=admin ip=10.0.0.1". One log call writes two lines, and line 2 looks like a genuine login by the admin.

### 3. A generic answer, and the details where only the team looks

The input is the same request. The result for the customer: status 500 and "Something went wrong. Quote reference ref-0001 to support." The result for the team: two log lines, the token printed as [REDACTED] because it is a Secret, and the error under [ref-0001] with the class name and a message whose password is [REDACTED].
Support finds the detail by the reference: the customer quotes ref-0001, and log line 2 carries it. The canary search finds no fake secret in the page or the log.
The same forged name, through the fixed logger, is one line: the line break is written as the two characters backslash and n.

### 4. Redaction: what it finds and what it misses

The input is seven texts, a to g. The result: a, the card 4111-1111-1111-1111, becomes [CARD ****1111]; b, the order number 4111111111111112, is left; c, d and e are masked by key name (password, api_key, authorization); f, a URL with a password, is masked; g, "the key is hunter2", is left as it was.
How the Luhn check is made: from the right, every second digit is doubled (9 is taken off a result over 9) and all are added. For 4111111111111111 the sum is 30, which ends in 0, so it is a card; for 4111111111111112 the sum is 31, so it is not.
A Secret in a message prints as token [REDACTED]; only reveal() gives the value.

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
