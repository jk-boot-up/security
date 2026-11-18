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
00:57 The Scenario
01:26 The Words
02:02 What the Letters Stand For
03:04 What the Letters Stand For (continued)
03:23 The id in Redis is the cookie
04:13 From the Redis id to the cookie
05:18 Act One — The session lives in Redis
05:34 Act One — The session lives in Redis
05:53 Act One — The session lives in Redis
06:15 The same planted cookie, two settings
07:17 Act Two — A new id at every sign-in
07:44 Two limits, and what Redis keeps
08:45 Act Three — How long a session lives
09:09 Act Three — How long a session lives
09:23 Ana's devices after each way of ending
10:19 Act Four — Signing out, everywhere, and the limit
10:33 Act Four — Signing out, everywhere, and the limit
10:56 One Redis for every server, and when it stops
11:48 Act Five — The bill
12:11 Act Five — The bill
12:27 What It Costs
13:04 Where You Have Seen It
13:32 How To Use It
14:11 The Java Classes
14:26 BankSecurity — the session cookie
14:40 BankSecurity — a new id at sign-in
14:51 BankSecurity — the request cache
15:03 AbsoluteTimeoutFilter — too old, however busy
15:16 BankController — sign out everywhere
15:27 SessionKeys — the cookie is the id in Base64
15:40 Thanks for Watching
```

## Tags

spring session, redis, session management, session fixation, spring security, owasp top 10, a07, java, api security, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/session-management-with-spring-session-and-redis-explained.mp4` |
| Captions | `video/session-management-with-spring-session-and-redis-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: API Security in Java - With Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (90)
- [ ] tags are at most 500 characters in total — ok (158)
- [ ] description is at most 5,000 characters — ok (538)
- [ ] chapters start at 00:00 and there are at least three
