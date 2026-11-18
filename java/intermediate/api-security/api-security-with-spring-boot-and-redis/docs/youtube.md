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
01:05 The Scenario
01:33 The Words
02:09 What the Letters Stand For
03:16 What the Letters Stand For (continued)
03:26 Four checks on one request
04:26 Every key in, every field out
05:23 Act One — The first version
05:39 Act One — The first version
05:57 Two named fields, and every other body refused
06:55 Act Two — A request model
07:09 Act Two — A request model
07:21 Act Two — A request model
07:33 Five fields out, and a ceiling on every page
08:28 Act Three — A response model and a page ceiling
08:38 Act Three — A response model and a page ceiling
08:51 A bucket per caller, kept in Redis
09:51 Act Four — The budget runs out, and refills
10:13 Act Four — The budget runs out, and refills
10:26 One budget for every server
11:17 Act Five — The bill
11:42 What It Costs
12:17 Where You Have Seen It
12:49 How To Use It
13:23 The Java Classes
13:37 NaiveAccountsController — @RequestBody on the entity
13:53 UpdateProfileRequest — named fields, Bean Validation
14:09 AccountView — what the app may see
14:24 SafeAccountsController — validate, and cap the page
14:40 RateLimitFilter — 429, or 503 when the counter is down
14:57 RedisRateLimiter — refill, take, or say how long
15:15 Thanks for Watching
```

## Tags

api security, mass assignment, bean validation, rate limiting, redis, lua, spring boot, owasp api top 10, java, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/api-security-with-spring-boot-and-redis-explained.mp4` |
| Captions | `video/api-security-with-spring-boot-and-redis-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: API Security in Java - With Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (88)
- [ ] tags are at most 500 characters in total — ok (153)
- [ ] description is at most 5,000 characters — ok (560)
- [ ] chapters start at 00:00 and there are at least three
