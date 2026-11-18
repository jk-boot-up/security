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
01:59 What the Letters Stand For
03:04 The id in the browser, the hash at the bank
04:04 Sixteen bytes become a 22-character id
05:20 Act One — A session id: random, and never stored as itself
05:45 Act One — A session id: random, and never stored as itself
06:12 The same planted id, two banks
07:13 Act Two — A new id at every sign-in and step-up
07:33 Act Two — A new id at every sign-in and step-up
07:52 Three clocks on one session
09:02 Act Three — How long a session lives
09:22 Act Three — How long a session lives
09:45 What each cookie attribute does, and what sign-out must do
10:52 Act Four — Signing out, everywhere, and the cookie
11:17 Act Four — Signing out, everywhere, and the cookie
11:31 Act Four — Signing out, everywhere, and the cookie
11:52 Two servers, one session
12:43 Act Five — The bill
13:15 Four Rules for a Session
13:39 Where You Have Seen It
14:04 How To Use It
14:30 The Java Classes
14:42 SessionIds — 16 random bytes, then SHA-256
14:57 Base64Steps — 6 bits, one character
15:09 SessionManager — found by the hash, in constant time
15:20 SessionManager — a new id at sign-in
15:35 SessionManager — two timeouts
15:50 SessionManager — the password again for a payment
16:01 SessionManager — sign out everywhere
16:11 Cookies — the Set-Cookie value
16:22 BankServer — sign-out ends it on the server
16:32 Thanks for Watching
```

## Tags

session management, session fixation, session timeout, secure cookies, httponly, samesite, owasp, java, application security, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/session-management-explained.mp4` |
| Captions | `video/session-management-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: API Security in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (71)
- [ ] tags are at most 500 characters in total — ok (167)
- [ ] description is at most 5,000 characters — ok (833)
- [ ] chapters start at 00:00 and there are at least three
