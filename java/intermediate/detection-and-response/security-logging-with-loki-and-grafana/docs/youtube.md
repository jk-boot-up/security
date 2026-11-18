# YouTube sheet — Security Logging with Loki and Grafana

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Security Logging with Loki and Grafana: LogQL, Labels, Alert Rules and Tenants (OWASP A09)

## Description

A real Loki 3.7 and Grafana 13 in Docker with the bank's eight security events as JSON lines: pushes answered with 204 and refused without a tenant, LogQL for the transfer, everything from the same address and failures counted by address, the cost of putting the user in a label (150 streams against 1), the Loki ruler firing on four failed logins from one address and a rule with a typo that never does, tenant isolation, an event ten days old refused, no delete API, and Grafana reading the same data through a data source that sends the tenant.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/intermediate/detection-and-response/security-logging-with-loki-and-grafana

Chapters:

```
00:00 Introduction
00:50 The Scenario
01:21 The Words
02:00 What the Letters Stand For
03:06 What the Letters Stand For (continued)
03:27 A LogQL query, part by part
04:24 A push, its tenant, and the four streams it fills
05:18 Act One — Shipping the events
05:44 Act One — Shipping the events
06:06 Everything from the transfer's address, in time order
06:50 Act Two — Asking questions in LogQL
07:06 Act Two — Asking questions in LogQL
07:28 Act Two — Asking questions in LogQL
07:45 One hundred and fifty users: as a label, and in the line
08:38 Act Three — Labels and content
08:58 Act Three — Labels and content
09:18 Failures per address against the rule's threshold
10:09 Act Four — Alerting
10:33 Act Four — Alerting
10:59 Tenants, an old event, a delete, and Grafana
11:55 Act Five — The bill
12:23 Act Five — The bill
12:46 What It Costs
13:20 Where You Have Seen It
13:53 How To Use It
14:25 The Java Classes
14:41 Event — fixed fields, in a fixed order
14:56 Loki — every request names its tenant
15:04 Loki — a push: labels, a time, and the line
15:17 Streams — one stream per distinct label set
15:35 Lab — the alert rule
15:48 Grafana — the tenant header lives in the data source
16:01 Thanks for Watching
```

## Tags

loki, grafana, logql, security logging, log alerting, multi tenancy, structured logs, siem, owasp top 10, a09, java, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/security-logging-with-loki-and-grafana-explained.mp4` |
| Captions | `video/security-logging-with-loki-and-grafana-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Detection and Response in Java - With Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (90)
- [ ] tags are at most 500 characters in total — ok (156)
- [ ] description is at most 5,000 characters — ok (547)
- [ ] chapters start at 00:00 and there are at least three
