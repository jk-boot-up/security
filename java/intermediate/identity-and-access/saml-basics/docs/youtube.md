# YouTube sheet — SAML Single Sign-On and Signature Wrapping

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

SAML Basics: Signed Assertions and Signature Wrapping in Java

## Description

SAML single sign-on in Java with real XML signatures (RSA-SHA256, enveloped, exclusive canonicalization): a shop that signs customers in through their bank, a provider that trusts the key inside the message and accepts a self-signed admin, XML signature wrapping that gets a pinned-key provider to check one assertion and read another, and a strict provider that pins the key, reads only what the signature covers, and checks expiry, audience, the request, replay and DOCTYPEs.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/intermediate/identity-and-access/saml-basics

Chapters:

```
00:00 Introduction
01:06 The Scenario
01:40 The Words
02:09 What the Letters Stand For
03:02 What the Letters Stand For (continued)
04:02 What the Letters Stand For (continued)
04:14 A signature answers two questions
05:06 The request, and the signed assertion that comes back
06:12 Act One — Signing in through the bank
06:41 Act One — Signing in through the bank
06:54 How the signature is made, step by step
08:08 Act Two — How the bank signs the assertion
08:37 Act Two — How the bank signs the assertion
09:07 Whose key made the signature?
10:07 Act Three — A valid signature is not the same as the bank's signature
10:29 Act Three — A valid signature is not the same as the bank's signature
10:44 The signature covers one element; the code reads another
11:41 Act Four — Signature wrapping: one assertion checked, another read
12:09 Act Four — Signature wrapping: one assertion checked, another read
12:23 A genuine signature, and the statement still checked
13:17 Act Five — The statement itself is checked
13:34 Act Five — The statement itself is checked
14:01 What It Costs
14:37 Where You Have Seen It
15:09 How To Use It
15:43 The Java Classes
15:59 Idp — an enveloped signature over #ID
16:17 SignatureSteps — the digest, then the signature
16:34 SignatureCheck — the key from the message, or the pinned key
16:48 PinnedKeyServiceProvider — two questions, two different elements
17:02 StrictServiceProvider — one assertion, the one that was signed
17:19 StrictServiceProvider — then the statement itself
17:33 Attacks — signature wrapping
17:46 Thanks for Watching
```

## Tags

saml, xml signature, signature wrapping, sso, xmldsig, java, application security, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/saml-basics-explained.mp4` |
| Captions | `video/saml-basics-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Identity and Access in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (61)
- [ ] tags are at most 500 characters in total — ok (126)
- [ ] description is at most 5,000 characters — ok (477)
- [ ] chapters start at 00:00 and there are at least three
