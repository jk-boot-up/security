# YouTube sheet — Kubernetes Secrets

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Kubernetes Secrets: Base64, Encryption at Rest, OpenBao and Short-Lived Tokens

## Description

Kubernetes Secrets for a bank on a real kind cluster: base64 is not encryption, reading the raw value from etcd before and after encryption at rest, OpenBao with a short-lived token, and bound ServiceAccount tokens against the old token Secret.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/advanced/kubernetes-and-container-security/kubernetes-secrets

Chapters:

```
00:00 Introduction
00:49 The Scenario
01:20 The Words
02:06 What the Letters Stand For
03:13 What the Letters Stand For (continued)
04:02 How base64 is undone, with no key
05:10 The Secret as typed, as the API returns it, and who reads it
06:12 Act One — Base64 is not encryption
06:46 Act One — Base64 is not encryption
06:57 The bytes etcd holds in front of the card key
08:04 Act Two — Raw etcd, before encryption
08:33 Act Two — Raw etcd, before encryption
08:39 Encrypted secrets at each step, and the prefix
09:47 Act Three — Encryption at rest
10:13 Act Three — Encryption at rest
10:29 Act Three — Encryption at rest
10:44 The login, its answer, and the token's ten seconds
11:45 Act Four — OpenBao and a 10 second token
12:12 Act Four — OpenBao and a 10 second token
12:27 The bound token and the old token Secret, side by side
13:26 Act Five — A bound ServiceAccount token
13:55 What It Costs
14:31 Who Does What
15:13 Where You Have Seen It
15:46 How To Use It
16:17 The Java Classes
16:33 Cluster — read a value straight out of etcd
16:48 Cluster — switch on encryption at rest
17:02 Encryption — aescbc first, identity last
17:17 Stored — encrypted means the marker is there
17:27 Stored — the bytes in front of a text
17:38 Base64Steps — 4 places of 6 bits, 3 bytes of 8
17:54 OpenBao — a role for one ServiceAccount, 10 second tokens
18:12 Jwt — lifetime and pod
18:26 Thanks for Watching
```

## Tags

kubernetes secrets, encryption at rest, etcd, openbao, vault, service account token, kind, kubernetes security, devsecops, java, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/kubernetes-secrets-explained.mp4` |
| Captions | `video/kubernetes-secrets-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Kubernetes and Container Security in Java - With Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (78)
- [ ] tags are at most 500 characters in total — ok (169)
- [ ] description is at most 5,000 characters — ok (244)
- [ ] chapters start at 00:00 and there are at least three
