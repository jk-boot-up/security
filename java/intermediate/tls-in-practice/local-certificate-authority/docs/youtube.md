# YouTube sheet — A Local Certificate Authority

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Build a Certificate Authority in Java: Issue, Revoke and Short-Lived Certificates

## Description

A local certificate authority in Java with real X.509 certificates, checked by the JDK's own validator: an offline root and a constrained issuing authority, a policy the issuer applies (lifetime, names, teams), signing requests that prove key possession, a name constraint that holds even when the policy is skipped, revocation lists that only help clients that check them and fail closed when stale, and 24-hour certificates renewed by automation instead.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/intermediate/tls-in-practice/local-certificate-authority

Chapters:

```
00:00 Introduction
00:59 The Scenario
01:31 The Words
02:13 What the Letters Stand For
03:06 What the Letters Stand For (continued)
03:32 Three certificates in a chain
04:32 The client's check, step by step
05:53 Act One — Running a certificate authority
06:11 Act One — Running a certificate authority
06:30 Act One — Running a certificate authority
06:45 Act One — Running a certificate authority
07:00 Act One — Running a certificate authority
07:20 A signing request, the checks, the certificate
08:29 Act Two — Issuing from a signing request
08:50 Act Two — Issuing from a signing request
09:11 Revocation lists over twenty days
10:19 Act Three — Revocation: taking a certificate back
10:38 Act Three — Revocation: taking a certificate back
11:01 Twenty-four-hour certificates, renewed at sixteen hours
12:05 Act Four — Short-lived certificates instead of revocation
12:32 Act Four — Short-lived certificates instead of revocation
12:50 The offline root key, and revoking a whole branch
13:52 Act Five — The bill
14:13 Act Five — The bill
14:33 What It Costs
15:04 Where You Have Seen It
15:39 How To Use It
16:13 The Java Classes
16:27 CertificateAuthority — the policy, applied by the issuer
16:45 CertificateAuthority — path length 0 and a name constraint
17:02 Csr — the request's own key verifies its signature
17:16 Validator — a fixed date, revocation on or off
17:35 ChainSteps — the name constraint step
17:54 ShortLived — renew at two thirds of the life
18:09 Thanks for Watching
```

## Tags

certificate authority, x509, crl, revocation, tls, java, application security, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/local-certificate-authority-explained.mp4` |
| Captions | `video/local-certificate-authority-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: TLS in Practice in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (81)
- [ ] tags are at most 500 characters in total — ok (122)
- [ ] description is at most 5,000 characters — ok (456)
- [ ] chapters start at 00:00 and there are at least three
