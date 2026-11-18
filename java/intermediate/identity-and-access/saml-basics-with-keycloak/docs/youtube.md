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
01:07 The Scenario
01:41 The Words
02:12 What the Letters Stand For
03:06 What the Letters Stand For (continued)
04:07 What the Letters Stand For (continued)
04:11 One key the shop trusts
04:55 The request, the signed response, and its two signatures
06:01 Act One — Signing in through the bank
06:25 Act One — Signing in through the bank
06:52 Three valid-looking signatures, one trusted key
07:45 Act Two — A valid signature is not the same as the bank's signature
08:06 Act Two — A valid signature is not the same as the bank's signature
08:22 A signed assertion, and an unsigned one beside it
09:12 Act Three — Signature wrapping: one assertion checked, another read
09:28 Act Three — Signature wrapping: one assertion checked, another read
09:48 Signed by the bank, and still refused
10:34 Act Four — The statement itself is checked
11:03 A key rotation at the bank, seen from the shop
11:45 Act Five — The bill: keys, metadata and XML
12:14 What It Costs
12:48 Where You Have Seen It
13:22 How To Use It
13:57 The Java Classes
14:11 Running — the one key the shop trusts
14:29 ShopSecurity — a tight clock, and a reason for every refusal
14:46 SignatureByHand — the digest, then RSA with the metadata key
15:06 SamlTools — a valid signature, with the attacker's key
15:24 KeycloakLab — a new signing key, with a higher priority
15:43 Thanks for Watching
```

## Tags

saml, single sign-on, spring security, keycloak, opensaml, xml signature wrapping, owasp top 10, a07, java, identity, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/saml-basics-with-keycloak-explained.mp4` |
| Captions | `video/saml-basics-with-keycloak-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Identity and Access in Java - With Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (93)
- [ ] tags are at most 500 characters in total — ok (158)
- [ ] description is at most 5,000 characters — ok (539)
- [ ] chapters start at 00:00 and there are at least three
