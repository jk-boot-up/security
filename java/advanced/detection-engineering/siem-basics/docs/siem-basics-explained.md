# SIEM Basics, Explained

## The idea in one sentence

A SIEM is only as good as its parsers, its clock and its joins.

## The 5 acts

### 1. Three formats, one shape

The input is three raw lines: {"ts":"2026-10-23T09:20:00Z","type":"login","user":"alice",...}, time=2026-10-23 14:55:00 +0530 db=ledger actor=alice ... rows=500000, and Oct 23 09:12:10 bastion-01 sshd[6040]: Failed password for alice from 203.0.113.5.
The result is one shape: time (UTC) 09:20:00, 09:25:00 and 09:12:10; source web, db and host; user alice in all three; action login, bulk-read and login; outcome success, success and failure. Over the day 623 web, 302 database and 506 host events come out; the 20 garbled database lines (such as time=garbled db=ledger actor=? stmt=0) are kept as dead letters.

### 2. One query on the common fields

The input is the same day, asked the same question twice. Before normalizing, the query uses the web's own field names, so it can only read the web: 121 failed logins. After normalizing, one query on the common fields reads every source: web 121, database 0, host 57, so 178 from 2 sources.

### 3. The clock decides an alert

The input is the database line time=2026-10-23 14:55:00 +0530 ... rows=500000, alice's web login at 09:20:00 UTC, and the rule: a web login, then a bulk read by the same user within 15 minutes.
How the time is made: with the offset honoured, 14:55:00 - 05:30 = 09:25:00 UTC; with it ignored, 14:55:00 is read as if it were UTC, and the event moves by 330 minutes.
The result: honoured, the login and the read are 5 minutes apart, 1 alert; ignored, 335 minutes apart, 0 alerts. The nightly report job reads 600000 rows at 02:00:00 with no web login before it and raises no alert.

### 4. Correlating across sources

The input is alice's events from 203.0.113.5: web, 8 failed logins from 09:12:00 to 09:14:20; host, 6 failed logins from 09:12:10 to 09:14:15; web, a successful login at 09:20:00.
How the rule counts: the window is 09:10:00 to 09:20:00. The web alone has 8 failures, under 10; the host alone has 6 and no success: no alert either way. Joined, 14 failures, then the success: an alert.
The result: one source at a time, 0 alerts; on the joined events, 1 alert about alice with 15 events behind it; everyone else, with up to 3 mistyped passwords before a success, 0 alerts.

### 5. From alerts to cases

The input is the alerts the two rules raised. The result is the cases: alerts about the same user are grouped, so the analyst gets one case about alice with 2 alerts and 17 events behind them, not two unrelated alerts.

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
