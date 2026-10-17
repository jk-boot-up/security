# YouTube sheet — Least Privilege with Spring Security

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Least Privilege with Spring Security (Deny by Default, @PreAuthorize, Grants)

## Description

Least privilege with Spring Security, in Java: the easy way to build a feature is to give it admin power. Explained with an online store, like a hotel that hands every employee the master key. We start a real Spring Boot server, see that a path with no rule is denied even for an admin, guard refunds with method security and see what PostAuthorize really does, enforce two people for a refund with a rule that no role can switch off, compare a stolen admin token with a stolen scoped service token using Spring Security's own authorization manager, and give a month-end grant that expires by itself on an injected clock.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/beginner/safe-habits/least-privilege-with-spring-security

Chapters:

```
00:00 Introduction
01:08 The Scenario
01:31 The Words
02:00 Act One — Deny by default: every path needs a rule
02:42 Act Two — Method security: the rule sits next to the code
03:26 Act Three — Separation of duties: two people for a refund
04:02 Act Four — A role hierarchy, and what a stolen credential can reach
04:42 Act Five — Access that expires by itself
05:14 From Open to Least Privilege
05:41 Who Does What
06:09 Where You Have Seen It
06:41 How To Use It
07:06 Thanks for Watching
```

## Tags

least privilege, spring security, preauthorize, separation of duties, role hierarchy, authorization, spring boot, java, cybersecurity for beginners

## Files

| What | File |
| --- | --- |
| Video | `video/least-privilege-with-spring-security-explained.mp4` |
| Captions | `video/least-privilege-with-spring-security-explained.srt` |
| Thumbnail | `docs/thumbnail.png` (1280×720) |

## Settings

Public with instant Premiere; not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Security Foundations in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (77)
- [ ] tags are at most 500 characters in total — ok (139)
- [ ] description is at most 5,000 characters — ok (621)
- [ ] chapters start at 00:00 and there are at least three
