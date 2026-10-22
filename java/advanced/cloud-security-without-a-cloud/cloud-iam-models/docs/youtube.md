# YouTube sheet — Cloud IAM Models

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Cloud IAM Models: Policy Evaluation, Roles, Boundaries, the Confused Deputy and Escalation Paths (Java)

## Description

A cloud permission evaluator written in Java 25 for the policy language of the big providers: identity policies, explicit deny, NotAction, wildcards, conditions and policy variables; resource policies and the difference between one account and two; organization policies; roles with trust policies that ask for a second factor; permission boundaries and session policies that only narrow; a resource policy that names a session; the confused deputy attack on a reporting service that assumes roles in its customers' accounts and the external id that stops it; the same idea between services with a source ARN; and a search for privilege escalation paths.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/advanced/cloud-security-without-a-cloud/cloud-iam-models

Chapters:

```
00:00 Introduction
01:52 The Scenario
02:12 Act One — Identity policies: default deny, allow, explicit deny
02:33 Act Two — Accounts: who has to agree
02:54 Act Three — Roles and the layers that narrow them
03:14 Act Four — The confused deputy
03:34 Act Five — The bill
04:01 What It Costs
04:25 Who Does What
04:49 Where You Have Seen It
05:01 How To Use It
05:24 Thanks for Watching
```

## Tags

cloud iam, iam policy, least privilege, confused deputy, permission boundary, sts assume role, privilege escalation, cloud security, aws iam, java

## Files

| What | File |
| --- | --- |
| Video | `video/cloud-iam-models-explained.mp4` |
| Captions | `video/cloud-iam-models-explained.srt` |
| Thumbnail | `docs/thumbnail.png` (1280×720) |

## Settings

Public with instant Premiere; not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Security Foundations in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — FIX (103)
- [ ] tags are at most 500 characters in total — ok (137)
- [ ] description is at most 5,000 characters — ok (654)
- [ ] chapters start at 00:00 and there are at least three
