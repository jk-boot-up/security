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
01:32 The Scenario
01:51 Act One — Expose everything
02:29 Act Two — What Spring Boot already does
03:06 Act Three — Hardened: another port, a short list, rules
03:34 Act Four — Who may read, and who may change
04:02 Act Five — The bill
04:35 What It Costs
05:02 Who Does What
05:25 Where You Have Seen It
05:38 How To Use It
06:04 Thanks for Watching
```

## Tags

spring boot actuator, security misconfiguration, owasp top 10, a02, heap dump, spring security, java, application security

## Files

| What | File |
| --- | --- |
| Video | `video/security-misconfiguration-with-spring-boot-actuator-explained.mp4` |
| Captions | `video/security-misconfiguration-with-spring-boot-actuator-explained.srt` |
| Thumbnail | `docs/thumbnail.png` (1280×720) |

## Settings

Public with instant Premiere; not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Security Foundations in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (75)
- [ ] tags are at most 500 characters in total — ok (115)
- [ ] description is at most 5,000 characters — ok (440)
- [ ] chapters start at 00:00 and there are at least three
