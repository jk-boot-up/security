# YouTube sheet — Security Chaos Engineering

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Security Chaos Engineering: Do Your Controls Fail Closed? Toxiproxy Experiments

## Description

Security chaos engineering in Java with Toxiproxy: a bank payments service whose authorization, fraud score, audit trail, rate limit and revocation check are tested with injected latency, hangs, resets, a dead dependency and a trickle, a first version that fails open, a hardened version that fails closed, alerts counted in failed probes and a scoreboard of hypotheses held and broken.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/advanced/resilience-and-recovery/security-chaos-engineering

Chapters:

```
00:00 Introduction
00:49 The Scenario
01:24 The Words
02:02 What the Letters Stand For
02:31 One call, two answers to silence
03:24 Five controls, five proxies, six faults
04:22 Act One — The service, the faults and the steady state
04:42 Act One — The service, the faults and the steady state
04:53 Act One — The service, the faults and the steady state
05:12 Approved while authorization fails
05:56 Act Two — Authorization: down, slow, reset
06:13 No score, and a trickle that misses the deadline
07:05 Act Three — The fraud score: a hang and a trickle
07:30 Twenty decisions while the audit sink is down
08:19 Act Four — The audit trail: the sink down, then a slow close
08:39 Forty payments from one account, and a revoked certificate
09:30 Act Five — The rate limit and the revocation check
09:44 Act Five — The rate limit and the revocation check
10:02 The probes, round by round
10:57 Act Six — Does anybody hear it?
11:12 Act Six — Does anybody hear it?
11:26 Twenty hypotheses, before and after
12:11 Act Seven — The scoreboard, and what is left
12:24 Act Seven — The scoreboard, and what is left
12:47 What It Costs
13:24 Where You Have Seen It
13:52 How To Use It
14:23 The Java Classes
14:36 BankPayments — no answer from authorization: deny, or carry on
14:49 BankPayments — no fraud score: hold for review, or approve unscreened
15:01 Http — the deadline covers the body too
15:12 SpooledAudit — write the journal, then send; if the journal fails, say no
15:23 Monitor — three failed probes in a row raise one alert
15:34 Toxiproxy — a fault is a disabled proxy or a toxic
15:45 Thanks for Watching
```

## Tags

security chaos engineering, toxiproxy, fail closed, fail open, resilience, audit log, rate limiting, ocsp, alerting, java, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/security-chaos-engineering-explained.mp4` |
| Captions | `video/security-chaos-engineering-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Resilience and Recovery in Java - With Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (79)
- [ ] tags are at most 500 characters in total — ok (163)
- [ ] description is at most 5,000 characters — ok (386)
- [ ] chapters start at 00:00 and there are at least three
