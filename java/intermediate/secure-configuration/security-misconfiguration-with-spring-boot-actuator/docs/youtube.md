# YouTube sheet — Security Misconfiguration with Spring Boot Actuator

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Spring Boot Actuator Misconfiguration (OWASP A02: Env, Heap Dump, Shutdown)

## Description

Security misconfiguration with Spring Boot Actuator, OWASP Top 10 A02, in an online store on Spring Boot 4.1: the tutorial setting that exposes everything, a payment key read from the environment endpoint and found again in a heap dump, what Spring Boot already hides and what it cannot, a hardened store with its own loopback port, a short endpoint list, login rules and a read-only loggers endpoint, and one added line that costs a check.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/intermediate/secure-configuration/security-misconfiguration-with-spring-boot-actuator

Chapters:

```
00:00 Introduction
00:58 The Scenario
01:25 The Words
01:59 What the Letters Stand For
03:16 One port, open to everyone
03:58 Seven requests, no login
05:01 Act One — Expose everything
05:20 Act One — Expose everything
05:24 Act One — Expose everything
05:46 Act One — Expose everything
05:55 Act One — Expose everything
06:03 Three settings, three different stores
07:00 Act Two — What Spring Boot already does
07:14 Act Two — What Spring Boot already does
07:25 Act Two — What Spring Boot already does
07:37 Two ports, a short list, and rules
08:27 Act Three — Hardened: another port, a short list, rules
08:46 Act Three — Hardened: another port, a short list, rules
09:07 Act Three — Hardened: another port, a short list, rules
09:16 Three logins, two endpoints, one write
10:05 Act Four — Who may read, and who may change
10:16 Act Four — Who may read, and who may change
10:30 One line costs one check
11:17 Act Five — The bill
11:44 What It Costs
12:24 Where You Have Seen It
12:56 How To Use It
13:32 The Java Classes
13:46 ShopController — a key injected from the settings
14:03 StoreSecurity — first match wins
14:18 Running — one profile per demo
14:32 ActuatorChecker — one check: send a request, judge the reply
14:48 Wire — searching a download without holding it
15:01 Thanks for Watching
```

## Tags

spring boot actuator, security misconfiguration, owasp top 10, a02, heap dump, spring security, java, application security, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/security-misconfiguration-with-spring-boot-actuator-explained.mp4` |
| Captions | `video/security-misconfiguration-with-spring-boot-actuator-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Secure Configuration in Java - With Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (75)
- [ ] tags are at most 500 characters in total — ok (166)
- [ ] description is at most 5,000 characters — ok (440)
- [ ] chapters start at 00:00 and there are at least three
