# YouTube sheet — API Security with Spring Boot and Redis

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

API Security with Spring Boot: Mass Assignment, Response Models and a Redis Rate Limiter

## Description

API security with Spring Boot 4.1 and Redis, from the OWASP API Top 10: a bank API that binds the request body to the stored account so a customer promotes themselves to admin, returns the whole record and every customer on a huge page, and answers a thousand requests in a loop; then request records with Bean Validation and unknown fields refused, response records and a page ceiling, and a token bucket in Redis written as one Lua script that gives one budget to every server, refuses with 429 and a Retry-After, and fails closed when Redis stops answering.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/intermediate/api-security/api-security-with-spring-boot-and-redis

Chapters:

```
00:00 Introduction
01:39 The Scenario
02:00 Act One — The first version
02:33 Act Two — A request model
02:59 Act Three — A response model and a page ceiling
03:26 Act Four — The budget runs out, and refills
03:54 Act Five — The bill
04:26 What It Costs
04:51 Who Does What
05:20 Where You Have Seen It
05:41 How To Use It
06:05 Thanks for Watching
```

## Tags

api security, mass assignment, bean validation, rate limiting, redis, lua, spring boot, owasp api top 10, java

## Files

| What | File |
| --- | --- |
| Video | `video/api-security-with-spring-boot-and-redis-explained.mp4` |
| Captions | `video/api-security-with-spring-boot-and-redis-explained.srt` |
| Thumbnail | `docs/thumbnail.png` (1280×720) |

## Settings

Public with instant Premiere; not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Security Foundations in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (88)
- [ ] tags are at most 500 characters in total — ok (102)
- [ ] description is at most 5,000 characters — ok (560)
- [ ] chapters start at 00:00 and there are at least three
