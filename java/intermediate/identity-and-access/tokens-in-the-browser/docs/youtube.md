# YouTube sheet — Tokens in the Browser

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Tokens in the Browser: Backend for Frontend and DPoP in Java (RFC 9449) - OWASP A07

## Description

Where should a single-page app keep its OAuth tokens? In Java with only the JDK, explained with an online bank's banking app, like a hotel guest who leaves the room key on the restaurant table instead of at reception. We put the access and refresh tokens in localStorage and let a third-party script on the page read them and use the refresh token ten days later, move the OAuth client to a backend for the frontend that keeps the tokens on the server and gives the browser only a __Host- cookie, stop requests started by another site with SameSite and a request header, bind tokens to a browser key with DPoP so a stolen token is refused without a fresh proof, and finish with the bill: one more server to run, a shared session store, and a script on the open page that can still act as the user. Checked against the RFC 8037 thumbprint example.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/intermediate/identity-and-access/tokens-in-the-browser

Chapters:

```
00:00 Introduction
01:05 The Scenario
01:27 The Words
02:00 What the Letters Stand For
02:53 What the Letters Stand For (continued)
03:43 What the Letters Stand For (continued)
04:37 What a script on the page can read
05:35 Two stolen tokens, and a month of access
06:36 Act One — Tokens kept in the browser
06:57 Act One — Tokens kept in the browser
07:24 A cookie in the browser, the tokens on the server
08:27 Act Two — A backend for the frontend keeps the tokens on the server
08:44 Act Two — A backend for the frontend keeps the tokens on the server
09:04 Who starts the transfer, and what goes with it
10:06 Act Three — Requests started by another site
10:22 Act Three — Requests started by another site
10:36 The key's thumbprint, and one proof
11:45 Act Four — Tokens bound to a key, with DPoP
11:59 Act Four — Tokens bound to a key, with DPoP
12:13 Act Four — Tokens bound to a key, with DPoP
12:31 Act Four — Tokens bound to a key, with DPoP
12:46 What the defences cost
13:46 Act Five — The bill
14:00 Act Five — The bill
14:19 Where Tokens Can Live, Best Last
14:53 Where You Have Seen It
15:19 How To Use It
15:53 The Java Classes
16:08 Browser — localStorage, and cookies without HttpOnly
16:24 Bff — sign in: keep the tokens, hand out a cookie
16:36 Bff — every call: check the header, add the token
16:49 AuthServer — five minutes, and the key's thumbprint
17:02 DpopKey — the thumbprint
17:18 DpopKey — one proof for one request
17:29 BankApi — compare the proof's key with the token's
17:44 DpopVerifier — this request, and only once
17:56 Thanks for Watching
```

## Tags

oauth, bff, backend for frontend, dpop, rfc 9449, single page app, localstorage, access token, java, application security, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/tokens-in-the-browser-explained.mp4` |
| Captions | `video/tokens-in-the-browser-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Identity and Access in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (83)
- [ ] tags are at most 500 characters in total — ok (163)
- [ ] description is at most 5,000 characters — ok (846)
- [ ] chapters start at 00:00 and there are at least three
