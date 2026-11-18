# YouTube sheet — Finding TLS Misconfigurations

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Finding TLS Misconfigurations in Java (Protocols, Ciphers, Certificates, HSTS)

## Description

A TLS configuration checker in Java for an online shop's servers: old protocols, broken and non-forward-secret cipher suites, expired, misnamed and incomplete certificate chains, weak keys and SHA-1 signatures, missing HSTS, redirects and Secure cookies, each a rule with a stable id and a severity, a report with a grade, and a move to Mozilla's intermediate policy that shows what a configuration can fix and what it cannot.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/intermediate/tls-in-practice/tls-misconfigurations

Chapters:

```
00:00 Introduction
01:01 The Scenario
01:36 The Words
02:23 What the Letters Stand For
03:34 What the Letters Stand For (continued)
04:38 What the Letters Stand For (continued)
04:50 What a cipher suite name says
05:49 What a scan learned about shop.example, and its report
06:46 Act One — A server that follows the guidance
07:05 Act One — A server that follows the guidance
07:34 Thirteen findings take a hundred points below zero
08:30 Act Two — A legacy server
08:53 Act Two — A legacy server
09:14 Act Two — A legacy server
09:31 Two certificates' dates around today, and the name checks
10:34 Act Three — A certificate that clients will refuse, and a forgotten HTTP setup
10:50 Act Three — A certificate that clients will refuse, and a forgotten HTTP setup
11:07 Act Three — A certificate that clients will refuse, and a forgotten HTTP setup
11:25 Act Three — A certificate that clients will refuse, and a forgotten HTTP setup
11:36 Which findings a policy fixes, and which it cannot
12:33 Act Four — Moving the legacy server to a policy
13:00 Act Four — Moving the legacy server to a policy
13:16 What It Costs
13:51 Where You Have Seen It
14:27 How To Use It
15:03 The Java Classes
15:14 Checker — a rule with an id and a severity
15:29 Checker — points, then a grade
15:40 Severity — the penalties
15:49 CipherSuites — forward secrecy from the name
16:03 SuiteName — split at _WITH_
16:18 Policy — replace protocols and suites, keep the rest
16:34 Thanks for Watching
```

## Tags

tls, ssl labs, cipher suites, hsts, certificate chain, misconfiguration, java, application security, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/tls-misconfigurations-explained.mp4` |
| Captions | `video/tls-misconfigurations-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: TLS in Practice in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (78)
- [ ] tags are at most 500 characters in total — ok (143)
- [ ] description is at most 5,000 characters — ok (426)
- [ ] chapters start at 00:00 and there are at least three
