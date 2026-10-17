# YouTube sheet — Authentication vs Authorization with Spring Security

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Authentication vs Authorization with Spring Security

## Description

Authentication vs authorization with Spring Security, in Java: authentication finds out who is asking, and authorization decides what that person may do. Explained with an online store, like a hotel where the front desk checks your passport and the key card decides which doors open. We use Spring Security's filter chain, BCrypt password storage and PreAuthorize rules, watch a login-only shop let a customer refund orders, see Spring answer 401 and 403, and finish with the cost: a forgotten rule is an open endpoint, so a second rule on the address is worth having, and HTTP Basic checks the password on every request.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/beginner/identity/authentication-vs-authorization-with-spring-security

Chapters:

```
00:00 Introduction
01:19 The Scenario
01:43 The Parts of Spring Security
02:18 Act One — Spring Security present, but every request permitted
02:55 Act Two — Authentication: HTTP Basic, passwords stored as BCrypt
03:46 Act Three — Authenticated is not the same as authorized
04:17 Act Four — Authorization: @PreAuthorize rules on the endpoints
05:15 Act Five — The bill: a forgotten rule, and a password check on every request
06:10 Plain Version, Spring Version
06:44 Who Does What
07:20 Where You Have Seen It
07:54 How To Use It
08:23 Thanks for Watching
```

## Tags

spring security, authentication, authorization, preauthorize, 401 vs 403, bcrypt, java, cybersecurity for beginners

## Files

| What | File |
| --- | --- |
| Video | `video/authentication-vs-authorization-with-spring-security-explained.mp4` |
| Captions | `video/authentication-vs-authorization-with-spring-security-explained.srt` |
| Thumbnail | `docs/thumbnail.png` (1280×720) |

## Settings

Public with instant Premiere; not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Security Foundations in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (52)
- [ ] tags are at most 500 characters in total — ok (108)
- [ ] description is at most 5,000 characters — ok (621)
- [ ] chapters start at 00:00 and there are at least three
