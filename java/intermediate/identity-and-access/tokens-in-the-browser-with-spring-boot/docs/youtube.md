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
00:56 The Scenario
01:24 The Words
01:57 What the Letters Stand For
02:58 What the Letters Stand For (continued)
03:57 What the Letters Stand For (continued)
04:51 What the Letters Stand For (continued)
04:57 What a script on the page can read
05:55 Two stolen tokens, as long as the session lives
07:04 Act One — Tokens kept in the browser
07:28 Act One — Tokens kept in the browser
07:52 A cookie in the browser, the tokens on the server
08:56 Act Two — A backend for the frontend keeps the tokens on the server
09:10 Act Two — A backend for the frontend keeps the tokens on the server
09:20 Act Two — A backend for the frontend keeps the tokens on the server
09:40 Who starts the request, and what goes with it
10:40 Act Three — Requests started by another site
10:58 Act Three — Requests started by another site
11:13 The key's thumbprint, and one proof
12:27 Act Four — Tokens bound to a key, with DPoP
12:44 Act Four — Tokens bound to a key, with DPoP
13:01 Act Four — Tokens bound to a key, with DPoP
13:22 Act Four — Tokens bound to a key, with DPoP
13:37 What the defences cost
14:36 Act Five — The bill
14:51 Act Five — The bill
15:10 What It Costs
15:45 Where You Have Seen It
16:22 How To Use It
16:59 The Java Classes
17:13 Running — the __Host-bff cookie
17:26 BffSecurity — oauth2Login, and a CSRF token in a cookie
17:43 BffController — the token never leaves the server
17:57 ApiSecurity — one line for DPoP
18:10 DpopKey — the thumbprint, by hand
18:24 DpopKey — one proof for one request
18:36 Thanks for Watching
```

## Tags

bff, backend for frontend, dpop, rfc 9449, spring security, keycloak, localstorage, oauth2, owasp top 10, a07, java, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/tokens-in-the-browser-with-spring-boot-explained.mp4` |
| Captions | `video/tokens-in-the-browser-with-spring-boot-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Identity and Access in Java - With Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (72)
- [ ] tags are at most 500 characters in total — ok (156)
- [ ] description is at most 5,000 characters — ok (614)
- [ ] chapters start at 00:00 and there are at least three
