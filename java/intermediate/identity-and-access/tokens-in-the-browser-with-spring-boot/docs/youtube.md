# YouTube sheet — Tokens in the Browser with Spring Boot: a BFF and DPoP

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Tokens in the Browser: Spring Boot BFF and DPoP with Keycloak (RFC 9449)

## Description

Tokens in the browser with Spring Boot 4.1, Spring Security 7.1 and Keycloak 26: a single-page app whose localStorage tokens are stolen by a third-party script, a backend for the frontend that holds the tokens on the server behind a __Host-bff cookie with an XSRF header, SameSite and preflight against requests another site starts, a refresh done on the server, then DPoP-bound tokens checked by Spring Security's resource server with proofs that are refused when copied, reused, old, for another request or from another key, and the bill: shared sessions, an extra hop, a lost key, and a script on the open page.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/intermediate/identity-and-access/tokens-in-the-browser-with-spring-boot

Chapters:

```
00:00 Introduction
01:44 The Scenario
02:07 Act One — Tokens kept in the browser
02:41 Act Two — A backend for the frontend keeps the tokens on the server
03:16 Act Three — Requests started by another site
03:52 Act Four — Tokens bound to a key, with DPoP
04:24 Act Five — The bill
04:55 What It Costs
05:22 Who Does What
05:56 Where You Have Seen It
06:13 How To Use It
06:35 Thanks for Watching
```

## Tags

bff, backend for frontend, dpop, rfc 9449, spring security, keycloak, localstorage, oauth2, owasp top 10, a07, java

## Files

| What | File |
| --- | --- |
| Video | `video/tokens-in-the-browser-with-spring-boot-explained.mp4` |
| Captions | `video/tokens-in-the-browser-with-spring-boot-explained.srt` |
| Thumbnail | `docs/thumbnail.png` (1280×720) |

## Settings

Public with instant Premiere; not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Security Foundations in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (72)
- [ ] tags are at most 500 characters in total — ok (105)
- [ ] description is at most 5,000 characters — ok (614)
- [ ] chapters start at 00:00 and there are at least three
