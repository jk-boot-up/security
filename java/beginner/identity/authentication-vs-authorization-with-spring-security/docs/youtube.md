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
00:57 The Scenario
01:17 The Words
01:48 What the Letters Stand For
02:28 What the Letters Stand For (continued)
02:44 One request, two questions
03:30 Installed, but told to let everyone in
04:10 Act One — Spring Security present, everything permitted
04:25 Who are you? The filter and the password check
05:19 Act Two — Authentication: HTTP Basic and BCrypt
05:36 How the login is written into every request
06:33 Act Three — The login on the wire: the HTTP Basic header
06:53 What BCrypt stores, and how a login is checked
07:47 Act Four — The password check, step by step: BCrypt
08:01 Act Four — The password check, step by step: BCrypt
08:13 The first question answered, the second never asked
08:51 Act Five — Authenticated is not the same as authorized
09:03 Three rules, eight requests
09:58 Act Six — Authorization: @PreAuthorize rules on the endpoints
10:04 Act Six — Authorization: @PreAuthorize rules on the endpoints
10:17 The bill: every request pays, and a forgotten rule
11:08 Act Seven — The bill: a forgotten rule, a check every time
11:23 How To Use It
11:49 The Java Classes
11:56 SpringAuthDemo — requests first, then responses
12:08 Shop — MockMvc, no network
12:26 LoginOnlyConfig — the filter chain
12:48 ShopController — the @PreAuthorize rules
13:02 LayeredConfig — the fix
13:11 BasicHeader — encoding, not encryption
13:21 HashSteps — hash again with the stored salt
13:39 CountingPasswordEncoder — count every check
13:49 Thanks for Watching
```

## Tags

spring security, authentication, authorization, preauthorize, 401 vs 403, bcrypt, java, cybersecurity for beginners, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/authentication-vs-authorization-with-spring-security-explained.mp4` |
| Captions | `video/authentication-vs-authorization-with-spring-security-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Identity Basics in Java - With Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (52)
- [ ] tags are at most 500 characters in total — ok (159)
- [ ] description is at most 5,000 characters — ok (621)
- [ ] chapters start at 00:00 and there are at least three
