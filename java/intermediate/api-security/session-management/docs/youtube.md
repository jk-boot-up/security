# YouTube sheet — Session Management: Ids, Rotation, Timeouts and Sign-Out

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Session Management in Java (Session Fixation, Timeouts, Secure Cookies)

## Description

Session management in Java: after a customer signs in, a session id in a cookie is all that proves who they are, so it is as good as a password for as long as it lives. Explained with an online bank. A session id is 128 random bits, stored only as its hash; a bank that keeps the same id at sign-in lets a stranger who planted that id read the account (session fixation), and issuing a new id at sign-in and at step-up stops it; idle and absolute timeouts end sessions, and payments need the password again; sign-out ends the session on the server, everywhere, and after a password change, with a limit on sessions; and the cookie carries Secure, HttpOnly, SameSite=Strict and the __Host- prefix. The bill: many servers need a shared session store, and timeouts cost convenience. Mapped to OWASP Top 10 2025 A07, CWE-384 and CWE-613.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/intermediate/api-security/session-management

Chapters:

```
00:00 Introduction
01:01 The Scenario
01:22 The Words
01:59 Act One — A session id: random, and never stored as itself
02:43 Act Two — A new id at every sign-in and step-up
03:26 Act Three — How long a session lives
04:07 Act Four — Signing out, everywhere, and the cookie
04:47 Act Five — The bill
05:28 Four Rules for a Session
05:52 Who Does What
06:19 Where You Have Seen It
06:44 How To Use It
07:10 Thanks for Watching
```

## Tags

session management, session fixation, session timeout, secure cookies, httponly, samesite, owasp, java, application security

## Files

| What | File |
| --- | --- |
| Video | `video/session-management-explained.mp4` |
| Captions | `video/session-management-explained.srt` |
| Thumbnail | `docs/thumbnail.png` (1280×720) |

## Settings

Public with instant Premiere; not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Security Foundations in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (71)
- [ ] tags are at most 500 characters in total — ok (116)
- [ ] description is at most 5,000 characters — ok (833)
- [ ] chapters start at 00:00 and there are at least three
