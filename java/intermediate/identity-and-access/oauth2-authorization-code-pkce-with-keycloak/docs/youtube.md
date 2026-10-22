# YouTube sheet — OAuth 2.0 Authorization Code with PKCE and Keycloak

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

OAuth 2.0 Authorization Code + PKCE with Keycloak: Redirects, State, Code Reuse, Refresh Rotation

## Description

OAuth 2.0 authorization code flow with PKCE against a real Keycloak 26 in Docker, with a Spring Security 7 resource server: the flow step by step with a consent screen, PKCE that burns a code at the first wrong try, exact redirect addresses (a path trick, a lookalike host, a query, upper case and a trailing slash are all refused), the state check Mallory's link fails, a code that works once and ends the session when reused, a code that expires, scopes the client was not registered for, refresh token rotation and reuse detection, the implicit flow and the password grant switched off, and a confidential client that still needs PKCE.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/intermediate/identity-and-access/oauth2-authorization-code-pkce-with-keycloak

Chapters:

```
00:00 Introduction
01:40 The Scenario
02:08 Act One — The flow, step by step
02:49 Act Two — PKCE: a code is useless without the verifier
03:20 Act Three — Exact redirects, state, and codes that work once
03:58 Act Four — Scopes and refresh tokens
04:29 Act Five — Current practice, and the bill
05:04 What It Costs
05:32 Who Does What
06:01 Where You Have Seen It
06:18 How To Use It
06:48 Thanks for Watching
```

## Tags

oauth2, pkce, keycloak, authorization code flow, spring security, resource server, rfc 9700, owasp top 10, a07, java

## Files

| What | File |
| --- | --- |
| Video | `video/oauth2-authorization-code-pkce-with-keycloak-explained.mp4` |
| Captions | `video/oauth2-authorization-code-pkce-with-keycloak-explained.srt` |
| Thumbnail | `docs/thumbnail.png` (1280×720) |

## Settings

Public with instant Premiere; not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Security Foundations in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (97)
- [ ] tags are at most 500 characters in total — ok (107)
- [ ] description is at most 5,000 characters — ok (638)
- [ ] chapters start at 00:00 and there are at least three
