# YouTube sheet — Cloud Misconfigurations

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Cloud Misconfigurations: Finding and Fixing Them on an AWS Emulator (Moto, Checkov, Trivy, IMDSv2)

## Description

Cloud misconfigurations on a local AWS emulator (Moto) for a bank: public buckets, a wildcard administrator policy, ports open to the world, the instance metadata service without a token and unencrypted disks. A scanner written in Java reads the account through the AWS SDK and finds 13 findings from 9 rules; the fixes are 7 edits and 2 replacements; the same setup as OpenTofu files is scanned by Checkov and Trivy at pinned versions and compared rule by rule; server-side request forgery against IMDSv1 and IMDSv2 is shown request by request; and the emulator accepts a public policy that AWS would refuse.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/advanced/cloud-security-without-a-cloud/cloud-misconfigurations

Chapters:

```
00:00 Introduction
01:01 The Scenario
01:44 The Words
02:36 What the Letters Stand For
03:38 What the Letters Stand For (continued)
04:44 What the Letters Stand For (continued)
05:14 One bucket: settings in, findings out
06:16 Thirteen findings, worst first
07:17 Act One — The first version, deployed to the emulator
07:48 Act One — The first version, deployed to the emulator
08:02 Act One — The first version, deployed to the emulator
08:14 Nine changes take thirteen findings to none
09:14 Act Two — The fixes, made with the same SDK
09:27 Act Two — The fixes, made with the same SDK
09:51 Act Two — The fixes, made with the same SDK
09:58 Our nine rules against Checkov and Trivy
11:01 Act Three — Before deploy: the same setup as files, read by two scanners
11:20 Act Three — Before deploy: the same setup as files, read by two scanners
11:36 Act Three — Before deploy: the same setup as files, read by two scanners
11:58 Three ways to the metadata service
12:56 Act Four — The metadata endpoint, reached through the web server
13:21 Act Four — The metadata endpoint, reached through the web server
13:36 Moto accepts what AWS refuses
14:25 Act Five — What the scanners and the emulator cannot tell
14:42 Act Five — What the scanners and the emulator cannot tell
15:05 What It Costs
15:44 Where You Have Seen It
16:18 How To Use It
16:56 The Java Classes
17:10 Scanner — who the ACL lets in
17:26 Rules — star on star
17:41 Rules — admin ports to the world
17:55 Environment — an edit, then a replacement
18:10 IacScanners — a pinned image, its own checks
18:23 IacScanners — the check that fired, or a dash
18:36 Imds — a token first, but not through a proxy
18:51 Thanks for Watching
```

## Tags

cloud misconfiguration, aws security, s3 public bucket, imdsv2, ssrf, checkov, trivy, moto, infrastructure as code, java, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/cloud-misconfigurations-explained.mp4` |
| Captions | `video/cloud-misconfigurations-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Cloud Security Without a Cloud in Java - With Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (98)
- [ ] tags are at most 500 characters in total — ok (162)
- [ ] description is at most 5,000 characters — ok (609)
- [ ] chapters start at 00:00 and there are at least three
