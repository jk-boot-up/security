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
02:11 Act One — Walking order numbers
02:52 Act Two — Routes that nobody checks
03:32 Act Three — Sideways and upwards
04:12 Act Four — One choke point, deny by default
04:54 Act Five — Keeping it right, and the bill
05:43 What Does Not Work, and What Does
06:13 Who Does What
06:42 Where You Have Seen It
07:14 How To Use It
07:39 Thanks for Watching
```

## Tags

broken access control, idor, bola, owasp top 10, authorization, access control, api security, java, application security

## Files

| What | File |
| --- | --- |
| Video | `video/broken-access-control-explained.mp4` |
| Captions | `video/broken-access-control-explained.srt` |
| Thumbnail | `docs/thumbnail.png` (1280×720) |

## Settings

Public with instant Premiere; not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Security Foundations in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (71)
- [ ] tags are at most 500 characters in total — ok (112)
- [ ] description is at most 5,000 characters — ok (660)
- [ ] chapters start at 00:00 and there are at least three
