# SIEM Basics, Explained

## The idea in one sentence

A SIEM is only as good as its parsers, its clock and its joins.

## The 5 acts

### 1. Collecting three sources

Three parsers read three formats. 623 web events, 302 database events and 506 host events come out. The 20 garbled lines of the database audit log do not match and are kept as dead letters, so that someone can count them and ask why.

### 2. Normalizing into one shape

Every event becomes the same record: time in UTC, source, host, user, address, action and outcome. A query written for the web application's field names finds 121 failed logins. One query on action login and outcome failure finds 178, from the web and the hosts.

### 3. The clock

The database writes local time with an offset of +0530. If the offset is ignored the bulk export moves by 330 minutes. The rule login then a bulk read within 15 minutes raises 1 alert when the offset is honoured and 0 when it is not. The nightly report job reads 600000 rows too, but has no web login before it, so it raises none.

### 4. Correlating across sources

Alice's account is attacked with 8 failures on the web and 6 on ssh, then a success. Neither source reaches 10 failures alone, so the rule per source raises 0 alerts. On the joined events there are 14 failures and a success from the same address: 1 alert with 15 events behind it. Ordinary mistyped passwords raise none.

### 5. From alerts to cases

The brute force alert and the login-then-bulk-read alert are about the same user on the same day. An analyst should get one case with both, not two tickets.

## The verdict

Parse every source into one shape, keep and count what does not parse, put every time on one clock and record where it came from, write rules that need more than one source, and group alerts into cases.

## How to recognise this in code you did not write

- A parser that drops what it cannot read.
- A log with no time zone.
- A threshold applied to one source.
- One ticket for each alert about the same person.

## Where you have already met this

- A log line with no time zone.
- A parser that silently drops what it cannot read.
- Three dashboards for one attack.
- An alert threshold applied to one source only.
