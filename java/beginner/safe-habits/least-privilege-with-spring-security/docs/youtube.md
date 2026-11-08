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
02:00 What the Letters Stand For
03:08 Where Spring Security says no
04:13 A path nobody wrote a rule for
05:18 Act One — Deny by default: every path needs a rule
05:28 Act One — Deny by default: every path needs a rule
06:03 Before the method, or after it
07:15 Act Two — Method security: the rule sits next to the code
07:27 Act Two — Method security: the rule sits next to the code
07:53 Two people for a refund: the requests and the responses
09:05 Act Three — Separation of duties: two people for a refund
09:29 The role hierarchy, and one stolen token
10:41 Act Four — A role hierarchy, and what a stolen credential can reach
11:05 A grant that ends by itself
12:07 Act Five — Access that expires by itself
12:25 From Open to Least Privilege
12:52 Where You Have Seen It
13:24 How To Use It
13:49 The Java Classes
14:02 Shop — the rule chain ends with denyAll
14:23 Shop — admin includes the other jobs
14:36 Refunds — before, or after, the method runs
14:53 Refunds — two halves for approve
15:06 RefundRules — not the person who asked
15:19 GrantAuthorization — held, or granted right now
15:36 BlastRadius — ask Spring Security about every action
15:52 Thanks for Watching
```

## Tags

least privilege, spring security, preauthorize, separation of duties, role hierarchy, authorization, spring boot, java, cybersecurity for beginners, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/least-privilege-with-spring-security-explained.mp4` |
| Captions | `video/least-privilege-with-spring-security-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Safe Habits in Java - With Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (77)
- [ ] tags are at most 500 characters in total — ok (190)
- [ ] description is at most 5,000 characters — ok (621)
- [ ] chapters start at 00:00 and there are at least three
