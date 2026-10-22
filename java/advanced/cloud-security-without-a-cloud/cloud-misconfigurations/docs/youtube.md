# YouTube sheet — Cloud Misconfigurations

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Cloud Misconfigurations: Finding and Fixing Them on an AWS Emulator (Moto, Checkov, Trivy, IMDSv2)

## Description

Cloud misconfigurations on a local AWS emulator for a bank: public buckets, a wildcard administrator policy, ports open to the world, the metadata service without a token and unencrypted disks, found by a scanner written in Java, fixed, and found again in OpenTofu files by Checkov and Trivy, with the metadata endpoint attacked and defended.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/advanced/cloud-security-without-a-cloud/cloud-misconfigurations

Chapters:

```
00:00 Introduction
02:07 The Scenario
02:24 Act One — The first version
02:44 Act Two — The fixes
02:59 Act Three — Before deploy: Checkov and Trivy
03:15 Act Four — The metadata endpoint
03:31 Act Five — The bill
03:45 What It Costs
04:06 Who Does What
04:27 Where You Have Seen It
04:37 How To Use It
05:00 Thanks for Watching
```

## Tags

cloud misconfiguration, aws security, s3 public bucket, imdsv2, ssrf, checkov, trivy, moto, infrastructure as code, java

## Files

| What | File |
| --- | --- |
| Video | `video/cloud-misconfigurations-explained.mp4` |
| Captions | `video/cloud-misconfigurations-explained.srt` |
| Thumbnail | `docs/thumbnail.png` (1280×720) |

## Settings

Public with instant Premiere; not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Security Foundations in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (98)
- [ ] tags are at most 500 characters in total — ok (111)
- [ ] description is at most 5,000 characters — ok (342)
- [ ] chapters start at 00:00 and there are at least three
