# YouTube sheet — Authentication vs Authorization with Keycloak

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Authentication vs Authorization with Keycloak (Spring Boot, JWT, OAuth 2)

## Description

Authentication vs authorization with Keycloak, in Java: let a login server do the logging in. Explained with an online store, like a theatre that sells tickets at one box office and only checks tickets at the doors. We see a Spring Boot resource server that holds no passwords and trusts only tokens signed by the login server, why no token gives 401 and the wrong role gives 403, how roles in a Keycloak token become authorities, why a correctly signed token still needs the right issuer and audience, what happens when a role is taken away but the token is still alive, and what it costs to end a token early. A real Keycloak in Docker repeats the checks in the integration tests.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/beginner/identity/authentication-vs-authorization-with-keycloak

Chapters:

```
00:00 Introduction
01:03 The Scenario
01:24 The Words
01:51 Act One — No login in the shop, only tokens
02:26 Act Two — Who is it, and what may they do
03:04 Act Three — A real signature is not enough
03:34 Act Four — The roles in a token are a copy
04:13 Act Five — Ending a token early, and the bill
04:54 What The Shop Checks
05:26 Who Does What
05:58 Where You Have Seen It
06:23 How To Use It
06:48 Thanks for Watching
```

## Tags

keycloak, oauth2, oidc, jwt, spring security, resource server, pkce, 401 vs 403, java, cybersecurity for beginners

## Files

| What | File |
| --- | --- |
| Video | `video/authentication-vs-authorization-with-keycloak-explained.mp4` |
| Captions | `video/authentication-vs-authorization-with-keycloak-explained.srt` |
| Thumbnail | `docs/thumbnail.png` (1280×720) |

## Settings

Public with instant Premiere; not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Security Foundations in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (73)
- [ ] tags are at most 500 characters in total — ok (105)
- [ ] description is at most 5,000 characters — ok (682)
- [ ] chapters start at 00:00 and there are at least three
