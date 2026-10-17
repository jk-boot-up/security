# YouTube sheet — Least Privilege with Terraform and LocalStack

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Least Privilege with Terraform and LocalStack (IAM Policies, Blast Radius, Boundaries)

## Description

Least privilege with Terraform and LocalStack, in Java: cloud permissions are code, so they can be checked like code. Explained with an online store, like a key cabinet where the nightly export gets a key to one drawer and not the master key. We write AWS IAM policies in Terraform, apply them with OpenTofu to a local AWS emulator in Docker, read the same policies back in Java and count the blast radius of each, see how wildcard matching and the difference between a bucket and its contents trips people up, watch an explicit deny and a permissions boundary narrow an over-broad policy, and run a linter that catches iam PassRole on every role. Everything runs on your own machine, with no cloud account.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/beginner/safe-habits/least-privilege-with-terraform

Chapters:

```
00:00 Introduction
01:03 The Scenario
01:26 The Words
01:52 Act One — Policies written as code
02:21 Act Two — How a request is matched
02:56 Act Three — A deny beats every allow
03:26 Act Four — A permissions boundary
03:50 Act Five — A linter, and the way to more power
04:25 From Wide to Narrow
04:51 Who Does What
05:21 Where You Have Seen It
05:46 How To Use It
06:12 Thanks for Watching
```

## Tags

least privilege, terraform, opentofu, localstack, iam policy, permissions boundary, blast radius, java, cybersecurity for beginners

## Files

| What | File |
| --- | --- |
| Video | `video/least-privilege-with-terraform-explained.mp4` |
| Captions | `video/least-privilege-with-terraform-explained.srt` |
| Thumbnail | `docs/thumbnail.png` (1280×720) |

## Settings

Public with instant Premiere; not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Security Foundations in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (86)
- [ ] tags are at most 500 characters in total — ok (123)
- [ ] description is at most 5,000 characters — ok (707)
- [ ] chapters start at 00:00 and there are at least three
