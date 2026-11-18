# YouTube sheet — Cloud IAM Models

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Cloud IAM Models in Java: Policy Evaluation, Roles, the Confused Deputy and Escalation Paths

## Description

A cloud permission evaluator written in Java 25 for the policy language of the big providers: identity policies, explicit deny, NotAction, wildcards, conditions and policy variables; resource policies and the difference between one account and two; organization policies; roles with trust policies that ask for a second factor; permission boundaries and session policies that only narrow; a resource policy that names a session; the confused deputy attack on a reporting service that assumes roles in its customers' accounts and the external id that stops it; the same idea between services with a source ARN; and a search for privilege escalation paths.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/advanced/cloud-security-without-a-cloud/cloud-iam-models

Chapters:

```
00:00 Introduction
00:56 The Scenario
01:31 The Words
02:15 What the Letters Stand For
03:08 What the Letters Stand For (continued)
03:43 What the Letters Stand For (continued)
03:51 How one request is decided
04:50 ana's policy, her six requests, and the answers
05:50 Act One — Identity policies: default deny, allow, explicit deny
06:05 Act One — Identity policies: default deny, allow, explicit deny
06:24 Act One — Identity policies: default deny, allow, explicit deny
06:47 Act One — Identity policies: default deny, allow, explicit deny
07:06 One account or two: who has to say yes
08:02 Act Two — Accounts: who has to agree
08:21 Act Two — Accounts: who has to agree
08:44 Eight actions through three layers
09:37 Act Three — Roles and the layers that narrow them
09:54 Act Three — Roles and the layers that narrow them
10:08 Act Three — Roles and the layers that narrow them
10:30 The deputy, before and after the external id
11:34 Act Four — The confused deputy
11:56 Act Four — The confused deputy
12:21 Act Four — The confused deputy
12:39 Three users, two paths to administrator
13:29 Act Five — The bill
13:42 Act Five — The bill
14:00 Act Five — The bill
14:22 What It Costs
14:58 Where You Have Seen It
15:28 How To Use It
16:01 The Java Classes
16:13 Evaluator — a deny anywhere wins
16:25 Evaluator — one account, or two
16:36 Wild — a pattern becomes a regular expression
16:47 Role — the trust policy is the role's resource policy
17:05 Steps — stop at the first answer that decides
17:19 Escalation — pass an admin role to a function
17:30 Thanks for Watching
```

## Tags

cloud iam, iam policy, least privilege, confused deputy, permission boundary, sts assume role, privilege escalation, cloud security, aws iam, java, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/cloud-iam-models-explained.mp4` |
| Captions | `video/cloud-iam-models-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Cloud Security Without a Cloud in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (92)
- [ ] tags are at most 500 characters in total — ok (188)
- [ ] description is at most 5,000 characters — ok (654)
- [ ] chapters start at 00:00 and there are at least three
