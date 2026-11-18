# YouTube sheet — OAuth 2.0 Authorization Code Flow with PKCE

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

OAuth 2.0 Authorization Code Flow with PKCE in Java (RFC 7636, RFC 9700)

## Description

The OAuth 2.0 authorization code flow with PKCE, in Java with only the JDK. Explained with an online bank and a budgeting app, like a hotel key card that the front desk issues for one room and one stay, without ever handing over the master key. We walk through every message of the flow, show that a leaked authorization code is useless without the PKCE code verifier, checked against the published example in RFC 7636, register exact redirect addresses so a code cannot be sent anywhere else, bind the state to the browser to stop login forgery, make codes single-use and short-lived, limit the app with scopes, rotate refresh tokens so that a stolen one revokes the whole family, and finish with current practice from RFC 9700: no implicit flow, no password grant, and a real authorization server in production.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/intermediate/identity-and-access/oauth2-authorization-code-pkce

Chapters:

```
00:00 Introduction
01:13 The Scenario
01:48 The Words
02:33 What the Letters Stand For
03:53 What the Letters Stand For (continued)
04:42 Two roads: the hash through the browser, the secret direct
05:37 Six messages between four parties
06:51 Act One — The authorization code flow, step by step
07:17 Act One — The authorization code flow, step by step
07:43 Act One — The authorization code flow, step by step
08:09 From the verifier to the challenge, step by step
09:24 Act Two — PKCE: a code is useless without the verifier
09:38 Act Two — PKCE: a code is useless without the verifier
10:00 Act Two — PKCE: a code is useless without the verifier
10:16 Act Two — PKCE: a code is useless without the verifier
10:29 Exact match, state, and a code that works once
11:31 Act Three — Exact redirect addresses, state, and codes that work once
11:48 Act Three — Exact redirect addresses, state, and codes that work once
12:02 Act Three — Exact redirect addresses, state, and codes that work once
12:22 One scope, and one family of tokens
13:24 Act Four — Scopes and refresh tokens
13:46 Act Four — Scopes and refresh tokens
14:05 What current practice removes, and what it keeps
15:08 Act Five — Current practice, and the bill
15:27 Act Five — Current practice, and the bill
15:57 Who Sees What
16:25 Where You Have Seen It
16:50 How To Use It
17:20 The Java Classes
17:33 Pkce — a hash of the secret, compared in constant time
17:51 ChallengeSteps — every 3 bytes become 4 characters of 6 bits
18:10 BudgetApp — the callback must carry this browser's state
18:24 ClientRegistration — exact redirect matching
18:36 AuthorizationServer — a code works once
18:53 AuthorizationServer — in time, and with the verifier
19:06 AuthorizationServer — refresh token rotation and reuse detection
19:19 BankApi — read is not pay
19:32 Thanks for Watching
```

## Tags

oauth 2.0, pkce, authorization code flow, rfc 9700, rfc 7636, refresh token rotation, redirect uri, api security, java, application security, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/oauth2-authorization-code-pkce-explained.mp4` |
| Captions | `video/oauth2-authorization-code-pkce-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Identity and Access in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (72)
- [ ] tags are at most 500 characters in total — ok (182)
- [ ] description is at most 5,000 characters — ok (813)
- [ ] chapters start at 00:00 and there are at least three
