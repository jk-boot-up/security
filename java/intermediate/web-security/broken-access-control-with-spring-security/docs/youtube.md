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
01:53 Act One — A Spring store with three common mistakes
02:43 Act Two — Deny by default in the filter chain
03:23 Act Three — The object-level check, in the query
04:10 Act Four — Method security, and a log of every refusal
04:55 Act Five — The route inventory, and the bill
05:38 Three Walls
06:02 Who Does What
06:34 Where You Have Seen It
07:01 How To Use It
07:25 Thanks for Watching
```

## Tags

spring security, broken access control, idor, bola, owasp top 10, authorization, spring boot, java, application security

## Files

| What | File |
| --- | --- |
| Video | `video/broken-access-control-with-spring-security-explained.mp4` |
| Captions | `video/broken-access-control-with-spring-security-explained.srt` |
| Thumbnail | `docs/thumbnail.png` (1280×720) |

## Settings

Public with instant Premiere; not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Security Foundations in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (95)
- [ ] tags are at most 500 characters in total — ok (112)
- [ ] description is at most 5,000 characters — ok (727)
- [ ] chapters start at 00:00 and there are at least three
