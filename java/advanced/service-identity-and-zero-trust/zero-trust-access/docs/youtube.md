# YouTube sheet — Zero Trust Access

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Zero Trust Access: Identity-Aware Proxy with Keycloak and OPA, Audit and Revocation

## Description

Zero trust access in Java with Keycloak and OPA: an identity-aware proxy that checks a JWT, asks a policy engine for a decision and writes an audit record for every request, compared with the network perimeter, plus token lifetime, revocation and what happens when a part is down.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/advanced/service-identity-and-zero-trust/zero-trust-access

Chapters:

```
00:00 Introduction
00:47 The Scenario
01:21 The Words
02:10 What the Letters Stand For
03:03 What the Letters Stand For (continued)
03:42 What the Letters Stand For (continued)
03:57 One request, three checks
05:06 Ten requests, no identity, ten answers
06:04 Act One — Before: the ledger trusts the network
06:21 Act One — Before: the ledger trusts the network
06:41 RS256, worked out for one token
08:00 Act Two — Who is calling: the token, checked by hand
08:24 Act Two — Who is calling: the token, checked by hand
08:50 Act Two — Who is calling: the token, checked by hand
09:13 Act Two — Who is calling: the token, checked by hand
09:25 One transfer, decided at 10:00 and at 23:00
10:32 Act Three — May they: OPA decides
10:56 Act Three — May they: OPA decides
11:09 Act Three — May they: OPA decides
11:37 Act Three — May they: OPA decides
11:49 How long a token works after a logout
13:01 Act Four — A token outlives its session
13:31 Act Four — A token outlives its session
13:45 Thirty-nine requests, thirty-nine records
14:41 Act Five — One record per request
15:01 Act Five — One record per request
15:16 What It Costs
15:58 Who Does What
16:46 Where You Have Seen It
17:17 How To Use It
17:50 The Java Classes
18:03 Proxy — 401, then 403, then forward
18:21 Proxy — fail closed, and always one record
18:35 Jwt — the verifier picks the algorithm
18:53 Rs256Steps — s^e mod n, then padding and hash
19:13 Opa — no answer is not a yes
19:24 Ledger — the proxy's mark
19:37 AuditLog — never given a token
19:52 Thanks for Watching
```

## Tags

zero trust, identity-aware proxy, keycloak, opa, rego, jwt, audit log, token revocation, policy as code, java, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/zero-trust-access-explained.mp4` |
| Captions | `video/zero-trust-access-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Service Identity and Zero Trust in Java - With Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (83)
- [ ] tags are at most 500 characters in total — ok (151)
- [ ] description is at most 5,000 characters — ok (280)
- [ ] chapters start at 00:00 and there are at least three
