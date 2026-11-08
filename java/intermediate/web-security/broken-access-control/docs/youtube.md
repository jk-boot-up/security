# YouTube sheet — Broken Access Control

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Broken Access Control in Java (IDOR, BOLA, Deny by Default) - OWASP A01

## Description

Broken access control in Java, the number one risk in the OWASP Top 10. Explained with an online store, like a hotel where every key card opens every door because the lock only checks that the card is a real one. We walk order numbers and read other customers' orders, open an admin report nobody linked to, send a refund that the page only hid, delete an account through a DELETE that was never checked, and change another customer's delivery address. Then we fix all of it with one policy at one choke point, deny by default, object-level ownership checks, a log of every refusal, and a route inventory test that fails the build when a new route has no rule.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/intermediate/web-security/broken-access-control

Chapters:

```
00:00 Introduction
01:10 The Scenario
01:38 The Words
02:11 What the Letters Stand For
03:17 What the Letters Stand For (continued)
03:42 One policy before every handler
04:47 Eleven order numbers, one signed-in customer
05:50 Act One — Walking order numbers
06:01 Act One — Walking order numbers
06:21 What the page shows, and what the server answers
07:13 Act Two — Routes that nobody checks
07:34 Nine attacks against the first version of the store
08:29 Act Three — Sideways and upwards
08:44 Act Three — Sideways and upwards
09:01 The same nine attacks, before and after the fix
09:51 Act Four — One choke point, deny by default
10:03 Act Four — One choke point, deny by default
10:18 Act Four — One choke point, deny by default
10:27 A route without a rule fails the build
11:18 Act Five — Keeping it right, and the bill
11:34 Act Five — Keeping it right, and the bill
11:58 Act Five — Keeping it right, and the bill
12:21 What Does Not Work, and What Does
12:51 Where You Have Seen It
13:23 How To Use It
13:47 The Java Classes
14:00 NaiveStore — checks written one by one
14:18 Policy — no rule means no
14:30 Policy — the owner check answers 404
14:47 GuardedStore — one choke point
15:00 GuardedStore — the route inventory
15:12 Thanks for Watching
```

## Tags

broken access control, idor, bola, owasp top 10, authorization, access control, api security, java, application security, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/broken-access-control-explained.mp4` |
| Captions | `video/broken-access-control-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Web Security in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (71)
- [ ] tags are at most 500 characters in total — ok (163)
- [ ] description is at most 5,000 characters — ok (660)
- [ ] chapters start at 00:00 and there are at least three
