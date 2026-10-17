# Security Logging, Explained

## The idea in one sentence

A security log records every security event with the same fields, never holds secrets, escapes the text it records, and carries a hash chain whose latest hash is kept elsewhere, so that it can answer who did what, cannot be used to steal or forge, and shows when it was changed.

## The 5 acts

### 1. What a security log must answer

The bank's old logger wrote 8 lines this morning, such as "ERROR login failed" and "INFO transfer done amount=500". The question is who moved 500 from ANA-1 to MAL-9, and from where. No old line even mentions ANA-1: the log cannot answer.
The security log records who, what, when, where from, the outcome and a request id for each of 8 events. The transfer was made as ana, from 198.51.100.66, at 09:05, request req-0006. The same address had 2 failed logins as ana, then 1 success:
someone with Ana's password, not Ana. The log covers logins, access decisions, money movements and admin changes.

### 2. What never to log

The old logger's card payment line holds the full card number, so a breach of the log is a breach of the cards. The security log's redactor removes the password tried in a failed login, the session id of the new login and the transfer's
Authorization header, and masks the card to ************1111, including a card number typed into a free-text note. Of the 5 secrets in the morning's events, 0 are found anywhere in the security log. Secrets are removed by field name, and card numbers and tokens also by their shape.

### 3. Text that tries to write the log

A username typed into the login form contains a line break followed by text that looks like a log line: "INFO login ok user=admin". In the old logger's file it becomes a second line, a forged record of an admin login. The security log
writes one JSON line, with the line break escaped; read back, the actor is exactly what was typed and the outcome is failure. A username carrying a quote and a fake outcome field cannot change the outcome either. Structured output escapes
text instead of trusting it, and keeps it whole, so investigators see exactly what was sent.

### 4. A log that shows it was changed

Each line carries the SHA-256 of the line before it, and the morning's 8 lines form an intact chain. Deleting line 6, the transfer, breaks the chain at line 6. Editing line 6's amount from 500 to 50 breaks it at line 7, the line after the edit.
An edited last line leaves the chain intact, because nothing follows it; comparing with the latest hash, published somewhere else as each line is written, finds the change. The file is written by appending only, with permissions rw-------. An attacker
with root can still rewrite the whole file and its chain, which is why the log must also leave the host.

### 5. The bill

Every line is also shipped to a second store as it is written; when the attacker deletes the local log, the local copy has 0 lines and the shipped one has all 8, with its chain intact. Names and addresses make the log personal data, so it is kept for a fixed time:
keeping 30 days, on day 41, 10 old lines go and 30 stay, and the kept lines still check from the anchor written down when the old ones went. The morning's lines average 291 bytes, so 2000000 logins a day write about 582 megabytes a day for logins alone. And
a log that nobody reads is evidence only after the fact: the -with-loki-and-grafana version ships, queries and alerts.

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
