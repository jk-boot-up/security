# YouTube sheet — SAML Basics with Keycloak

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

SAML with Spring Security and Keycloak: Signature Wrapping, Replay, Audience and Key Rotation

## Description

SAML 2.0 single sign-on with Spring Security 7 and a real Keycloak 26 in Docker: the authentication request and the signed response, an edited name, a response self-signed with a valid key of the attacker's, a genuine response signed by another realm, signature wrapping with an unsigned admin assertion before or after the signed one, no signature, a DOCTYPE, replay, an assertion that has expired, a response the bank signed for another shop, and a key rotation at the bank that refuses every shopper until the shop reloads its metadata.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/intermediate/identity-and-access/saml-basics-with-keycloak

Chapters:

```
00:00 Introduction
01:33 The Scenario
01:57 Act One — Signing in through the bank
02:24 Act Two — A valid signature is not the same as the bank's signature
02:51 Act Three — A valid signature on one assertion, and another one read
03:17 Act Four — The statement itself is checked
03:46 Act Five — The bill: keys, metadata and XML
04:11 What It Costs
04:36 Who Does What
05:04 Where You Have Seen It
05:18 How To Use It
05:40 Thanks for Watching
```

## Tags

saml, single sign-on, spring security, keycloak, opensaml, xml signature wrapping, owasp top 10, a07, java, identity

## Files

| What | File |
| --- | --- |
| Video | `video/saml-basics-with-keycloak-explained.mp4` |
| Captions | `video/saml-basics-with-keycloak-explained.srt` |
| Thumbnail | `docs/thumbnail.png` (1280×720) |

## Settings

Public with instant Premiere; not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Security Foundations in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (93)
- [ ] tags are at most 500 characters in total — ok (107)
- [ ] description is at most 5,000 characters — ok (539)
- [ ] chapters start at 00:00 and there are at least three
