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
01:39 The Scenario
01:56 Act One — Shipping the events
02:14 Act Two — Asking questions in LogQL
02:35 Act Three — Labels and content
02:52 Act Four — Alerting
03:11 Act Five — The bill
03:31 What It Costs
03:53 Who Does What
04:14 Where You Have Seen It
04:29 How To Use It
04:50 Thanks for Watching
```

## Tags

loki, grafana, logql, security logging, log alerting, multi tenancy, structured logs, siem, owasp top 10, a09, java

## Files

| What | File |
| --- | --- |
| Video | `video/security-logging-with-loki-and-grafana-explained.mp4` |
| Captions | `video/security-logging-with-loki-and-grafana-explained.srt` |
| Thumbnail | `docs/thumbnail.png` (1280×720) |

## Settings

Public with instant Premiere; not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Security Foundations in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (90)
- [ ] tags are at most 500 characters in total — ok (105)
- [ ] description is at most 5,000 characters — ok (547)
- [ ] chapters start at 00:00 and there are at least three
