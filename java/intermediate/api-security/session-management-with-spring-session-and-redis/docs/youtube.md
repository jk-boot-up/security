# YouTube sheet — Session Management with Spring Session and Redis

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Sessions in Redis with Spring Session: Fixation, Timeouts, Sign-Out Everywhere (OWASP A07)

## Description

Session management with Spring Session and Redis in Spring Boot 4.1: a bank whose sessions live in a password-protected Redis, why a stolen copy of the keys is a set of live sessions, session fixation and Spring Security's new id at sign-in, the idle timeout and the absolute timeout that Spring Session does not have, signing out, signing out everywhere, a password change and a limit on sessions, two servers sharing one store, a bank that fails closed when Redis stops answering, and a stranger filling Redis through the request cache.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/intermediate/api-security/session-management-with-spring-session-and-redis

Chapters:

```
00:00 Introduction
01:32 The Scenario
01:57 Act One — The session lives in Redis
02:26 Act Two — A new id at every sign-in
02:54 Act Three — How long a session lives
03:23 Act Four — Signing out, everywhere, and the limit
03:56 Act Five — The bill
04:24 What It Costs
04:50 Who Does What
05:15 Where You Have Seen It
05:33 How To Use It
05:53 Thanks for Watching
```

## Tags

spring session, redis, session management, session fixation, spring security, owasp top 10, a07, java, api security

## Files

| What | File |
| --- | --- |
| Video | `video/session-management-with-spring-session-and-redis-explained.mp4` |
| Captions | `video/session-management-with-spring-session-and-redis-explained.srt` |
| Thumbnail | `docs/thumbnail.png` (1280×720) |

## Settings

Public with instant Premiere; not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Security Foundations in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (90)
- [ ] tags are at most 500 characters in total — ok (107)
- [ ] description is at most 5,000 characters — ok (538)
- [ ] chapters start at 00:00 and there are at least three
