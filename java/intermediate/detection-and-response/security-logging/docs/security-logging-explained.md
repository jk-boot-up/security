# Security Logging, Explained

## The idea in one sentence

A security log records every security event with the same fields, never holds secrets, escapes the text it records, and carries a hash chain whose latest hash is kept elsewhere, so that it can answer who did what, cannot be used to steal or forge, and shows when it was changed.

## The 5 acts

### 1. What a security log must answer

The input is the bank's old log: 8 lines this morning, such as "ERROR login failed" and "INFO transfer done amount=500", and the question: who moved 500 from ANA-1 to MAL-9, and from where? No old line even mentions ANA-1, so the old log cannot answer.
The result is the security log. The same transfer, line 6, has eight fields: time, type, actor, action, target, source, outcome and correlation (the request id req-0006). Every event as ana shows two addresses: her usual 203.0.113.7, and 198.51.100.66, where
2 logins failed, 1 succeeded and then the transfer was made. Someone had Ana's password; it was not Ana. The log holds four event types: login, access-decision, money-movement and admin-change.

### 2. What never to log

The input: the old logger's card payment line holds the full card number, so a leaked log leaks the card; and the events' details hold five fake secrets: a tried password, a session id, a bearer token in the Authorization header, a card number, and the same card
number typed into a free-text note.
The result: after the redactor, the password, the session and the authorization are [removed], by their field names. The card numbers are caught by their shape, 13 to 19 digits: 4111111111111111 has 16 digits, so 12 stars and the last four, ************1111,
and the note's hyphenated number is masked the same way. A search finds 0 of the 5 secrets in the security log.

### 3. Text that tries to write the log

The input: a username typed into the login form, "mallory", a line break, and text that looks like a log line, "2026-10-18T09:09:00Z INFO login ok user=admin".
The result in the old log: one failed login becomes two lines in the file, and the second is a forged admin login. In the security log: one line, whose actor field holds the line break escaped as \n; read back, the actor is exactly what was typed and the outcome is failure.
A second input, mallory","outcome":"success, has every quote escaped with a backslash, so the fake field stays inside the actor and the outcome still reads failure. A line break becomes \n, a quote \", a backslash \\.

### 4. A log that shows it was changed

The input is the morning's 8 lines, written to an owner-only file by appending. Each line's prev field is the SHA-256 of the line before (line 1 starts from all zeros), and the head, the SHA-256 of line 8, is kept elsewhere.
The result: the untouched log is intact. Editing line 6's amount from 500 to 50 changes line 6's hash (3d32ec7f52d2 becomes 347bfe928ef5) while line 7's prev still names the old one, so the chain is broken at line 7. Deleting line 6 breaks it at line 6.
Editing line 8 leaves the chain intact, because nothing follows it; compared with the head, it does not match, so the change is found. Root can still rewrite the file and its whole chain, which is why the log must also leave the host.

### 5. The bill

The input: every line goes to two stores as it is written, and the attacker deletes the local log. The result: local 0 lines, shipped 8 lines, chain intact.
Retention: one login a day for 40 days from 2026-01-01, keeping 30 days. Today is 2026-02-10 (day 41), so the cutoff is today - 30 days = 2026-01-11: 10 lines (day 1 to 10) are removed and 30 kept, and the kept lines check from the anchor, the first kept line's prev.
The volume: the morning's 8 lines are 2325 bytes, 2325 / 8 = 291 bytes a line; 2000000 logins a day x 291 = 582000000 bytes, about 582 megabytes a day for logins alone. And a log nobody reads explains a breach only afterwards: the -with-loki-and-grafana version ships, queries and alerts.

## The verdict

Log logins, access decisions, money movements and admin changes as structured events with a request id. Redact by field name and by shape, and test the redactor with every secret the application handles. Write JSON, never pasted text.
Chain the lines, publish the head elsewhere, ship every line off the host as it is written, set a retention period, and budget the volume. Then watch the log, because a log nobody reads is only evidence.

## How to recognise this in code you did not write

- JSON log lines with `trace_id` or `request_id` fields.
- Audit-log requirements in payment and banking standards.
- Log masking settings for card numbers and tokens.
- Write-once log storage and log-shipping agents.

## Where you have already met this

- Audit logs in banking, card payment (PCI DSS) and health systems, which must say who accessed what and when.
- JSON log lines in cloud log services, with a trace or request id on every line.
- Breach reports that say the attackers were inside for months and nobody noticed.
- Incidents where passwords or tokens were found in plain text in log files.
