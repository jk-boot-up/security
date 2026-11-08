# YouTube sheet — Broken Access Control with Spring Security

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Broken Access Control with Spring Security (Deny by Default, BOLA, Route Inventory) - OWASP A01

## Description

Broken access control with Spring Security, OWASP A01, in Java. Explained with an online store, like a building with a good lock on every door the architect drew, and no lock on the doors added later. A real Spring Boot 4 store with three common mistakes lets seven of nine attacks through: a sign-in rule where an ownership rule was needed, a pattern that covers /admin/users but not /admin/users/ben, and permitAll() at the end. Then it is fixed with deny by default in the filter chain, the owner in the query, @PreAuthorize and @PostAuthorize compared, a log of every refusal from AuthorizationDeniedEvent, and a route inventory test that reads Spring MVC's own handler mapping and fails the build when a route has no rule.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/intermediate/web-security/broken-access-control-with-spring-security

Chapters:

```
00:00 Introduction
01:04 The Scenario
01:25 The Words
01:53 What the Letters Stand For
03:00 What the Letters Stand For (continued)
03:29 Three places Spring can refuse a request
04:30 Three rules, and what they let through
05:35 Act One — A Spring store with three common mistakes
05:48 Act One — A Spring store with three common mistakes
06:06 Every route has a rule, and the last rule is denyAll()
07:00 Act Two — Deny by default in the filter chain
07:29 Two ways to check the owner: in the query, or after it
08:26 Act Three — The object-level check, in the query
08:46 Act Three — The object-level check, in the query
09:04 A rule on the method holds without any URL
09:54 Act Four — Method security, and a log of every refusal
10:07 Act Four — Method security, and a log of every refusal
10:27 Spring lists the routes; a test checks each has a rule
11:12 Act Five — The route inventory, and the bill
11:42 Three Walls
12:07 Where You Have Seen It
12:35 How To Use It
12:59 The Java Classes
13:10 NaiveSecurity — rules that say too little
13:27 GuardedSecurity — deny by default
13:41 OrderAccess — the owner in the query
13:54 OrderAccess — the fallback: after the method
14:07 RefundService — method security
14:17 RouteInventory — the inventory test's question
14:29 Thanks for Watching
```

## Tags

spring security, broken access control, idor, bola, owasp top 10, authorization, spring boot, java, application security, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/broken-access-control-with-spring-security-explained.mp4` |
| Captions | `video/broken-access-control-with-spring-security-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Web Security in Java - With Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (95)
- [ ] tags are at most 500 characters in total — ok (163)
- [ ] description is at most 5,000 characters — ok (727)
- [ ] chapters start at 00:00 and there are at least three
