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
01:02 The Scenario
01:21 The Words
01:50 Act One — The authorization code flow, step by step
02:42 Act Two — PKCE: a code is useless without the verifier
03:35 Act Three — Exact redirect addresses, state, and codes that work once
04:34 Act Four — Scopes and refresh tokens
05:28 Act Five — Current practice, and the bill
06:18 Who Sees What
06:46 Who Does What
07:20 Where You Have Seen It
07:44 How To Use It
08:12 Thanks for Watching
```

## Tags

oauth 2.0, pkce, authorization code flow, rfc 9700, rfc 7636, refresh token rotation, redirect uri, api security, java, application security

## Files

| What | File |
| --- | --- |
| Video | `video/oauth2-authorization-code-pkce-explained.mp4` |
| Captions | `video/oauth2-authorization-code-pkce-explained.srt` |
| Thumbnail | `docs/thumbnail.png` (1280×720) |

## Settings

Public with instant Premiere; not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Security Foundations in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (72)
- [ ] tags are at most 500 characters in total — ok (131)
- [ ] description is at most 5,000 characters — ok (813)
- [ ] chapters start at 00:00 and there are at least three
