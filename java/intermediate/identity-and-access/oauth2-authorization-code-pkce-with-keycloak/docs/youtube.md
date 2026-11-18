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
01:06 The Scenario
01:37 The Words
02:20 What the Letters Stand For
03:35 What the Letters Stand For (continued)
04:36 What the Letters Stand For (continued)
04:51 Two roads: the hash through the browser, the secret direct
05:47 Six messages between four parties
06:57 Act One — The flow, step by step
07:22 Act One — The flow, step by step
07:38 Act One — The flow, step by step
07:56 Act One — The flow, step by step
08:20 From the verifier to the challenge, step by step
09:33 Act Two — PKCE: a code is useless without the verifier
09:46 Act Two — PKCE: a code is useless without the verifier
10:03 Act Two — PKCE: a code is useless without the verifier
10:30 Act Two — PKCE: a code is useless without the verifier
10:40 Exact match, state, and a code that works once
11:38 Act Three — Exact redirect addresses, state, and codes that work once
11:56 Act Three — Exact redirect addresses, state, and codes that work once
12:10 Act Three — Exact redirect addresses, state, and codes that work once
12:34 One scope, and one family of tokens
13:28 Act Four — Scopes and refresh tokens
13:46 Act Four — Scopes and refresh tokens
14:09 What current practice removes, and what it keeps
15:03 Act Five — Current practice, and the bill
15:21 Act Five — Current practice, and the bill
15:37 Act Five — Current practice, and the bill
15:56 What It Costs
16:32 Where You Have Seen It
17:05 How To Use It
17:41 The Java Classes
17:55 KeycloakLab — one container, the realm file imported
18:16 OAuthClient — the challenge, and the state check
18:31 ChallengeSteps — every 3 bytes become 4 characters of 6 bits
18:47 ApiSecurity — the key, the issuer, the audience and the dates
19:09 ApiSecurity — the scope decides what a token may do
19:18 Thanks for Watching
```

## Tags

oauth2, pkce, keycloak, authorization code flow, spring security, resource server, rfc 9700, owasp top 10, a07, java, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/oauth2-authorization-code-pkce-with-keycloak-explained.mp4` |
| Captions | `video/oauth2-authorization-code-pkce-with-keycloak-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Identity and Access in Java - With Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (97)
- [ ] tags are at most 500 characters in total — ok (158)
- [ ] description is at most 5,000 characters — ok (638)
- [ ] chapters start at 00:00 and there are at least three
