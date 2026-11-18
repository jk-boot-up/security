# YouTube sheet — Workload Identity

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Workload Identity with SPIFFE and SPIRE: Short-Lived SVIDs Instead of Shared Secrets

## Description

Workload identity for a bank's services with SPIFFE and SPIRE: a shared secret that leaks, SPIFFE IDs chosen by registration entries, 20 second X.509 SVIDs, mutual verification between two Java services, automatic rotation and a stolen SVID.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/advanced/service-identity-and-zero-trust/workload-identity

Chapters:

```
00:00 Introduction
00:48 The Scenario
01:18 The Words
02:06 What the Letters Stand For
02:59 What the Letters Stand For (continued)
03:40 Who decides a workload's name
04:36 One key, and the caller's own word
05:36 Act One — One shared secret in three services
06:03 Act One — One shared secret in three services
06:18 A SPIFFE ID and an SVID's dates, part by part
07:13 Act Two — SPIRE decides who a workload is
07:40 Act Two — SPIRE decides who a workload is
07:58 Both ends check the other's SPIFFE ID
08:53 Act Three — Two Java services check each other's SPIFFE ID
09:14 Act Three — Two Java services check each other's SPIFFE ID
09:25 Fifty seconds of calls, and SVIDs that live twenty
10:17 Act Four — Rotation: an SVID that lives 20 seconds
10:49 The stolen copy against the clock
11:42 Act Five — A stolen SVID
12:06 No entry, no identity
12:58 Act Six — A workload that matches no entry
13:20 What it takes to evict the attacker
14:12 Act Seven — Evicting the attacker
14:35 What It Costs
15:11 Who Does What
15:54 Where You Have Seen It
16:25 How To Use It
16:51 The Java Classes
17:05 LegacyLedger — the key, then the claimed name
17:14 Lab — a registration entry: a user number and an ID
17:28 Workload — the ledger reads the ID from the certificate
17:39 Workload — the client accepts one server ID
17:54 Policy — anything not listed is refused
18:05 SpiffeIds — trust domain, then path
18:18 Rotations — count the serial number changes
18:28 Thanks for Watching
```

## Tags

spiffe, spire, workload identity, svid, mtls, zero trust, service identity, x509, secret rotation, java, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/workload-identity-explained.mp4` |
| Captions | `video/workload-identity-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Service Identity and Zero Trust in Java - With Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (84)
- [ ] tags are at most 500 characters in total — ok (145)
- [ ] description is at most 5,000 characters — ok (241)
- [ ] chapters start at 00:00 and there are at least three
