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
02:00 Act One — Tokens kept in the browser
02:51 Act Two — A backend for the frontend keeps the tokens on the server
03:43 Act Three — Requests started by another site
04:25 Act Four — Tokens bound to a key, with DPoP
05:23 Act Five — The bill
06:08 Where Tokens Can Live, Best Last
06:43 Who Does What
07:13 Where You Have Seen It
07:39 How To Use It
08:13 Thanks for Watching
```

## Tags

oauth, bff, backend for frontend, dpop, rfc 9449, single page app, localstorage, access token, java, application security

## Files

| What | File |
| --- | --- |
| Video | `video/tokens-in-the-browser-explained.mp4` |
| Captions | `video/tokens-in-the-browser-explained.srt` |
| Thumbnail | `docs/thumbnail.png` (1280×720) |

## Settings

Public with instant Premiere; not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Security Foundations in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (83)
- [ ] tags are at most 500 characters in total — ok (112)
- [ ] description is at most 5,000 characters — ok (846)
- [ ] chapters start at 00:00 and there are at least three
