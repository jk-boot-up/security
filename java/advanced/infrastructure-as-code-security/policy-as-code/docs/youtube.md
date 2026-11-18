# YouTube sheet — Policy as Code

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Policy as Code: Deny-by-Default Rego Rules, Testing the Policy and Its Tests (OPA, Conftest)

## Description

Policy as code for a bank's Kubernetes deployments: a deny-by-default policy in Rego, a table of requests it must decide as a reviewer expects, the holes a missing field leaves, tests for the policy and mutation testing of those tests, an admission review, Conftest in the build and a warning-first rollout.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/advanced/infrastructure-as-code-security/policy-as-code

Chapters:

```
00:00 Introduction
00:52 The Scenario
01:25 The Words
02:11 What the Letters Stand For
03:04 One request through every rule
04:07 Thirteen requests: the reviewer and the policy
05:12 Act One — Deny by default
05:37 Act One — Deny by default
06:02 Act One — Deny by default
06:09 A missing field is undefined, and undefined denies nothing
07:32 Act Two — The holes: a missing field
07:58 Act Two — The holes: a missing field
08:25 Switch off one rule at a time: which tests notice?
09:31 Act Three — Testing the policy and its tests
09:56 Act Three — Testing the policy and its tests
10:07 One policy file, in the cluster and in the build
11:18 Act Four — In the cluster and in the build
11:36 Act Four — In the cluster and in the build
11:53 Act Four — In the cluster and in the build
12:08 What It Costs
12:41 Where You Have Seen It
13:12 How To Use It
13:45 The Java Classes
14:00 Cases — a change to the good request, and the rules it breaks
14:18 Opa — the deny messages for one input
14:31 Opa — the versions are pinned
14:43 Steps — one rule per step, so opa shows each value
14:57 Mutants — rename deny, and the rule is switched off
15:07 Mutants — a mutant is noticed when a test fails
15:19 Admission — no messages, allowed
15:28 Thanks for Watching
```

## Tags

policy as code, open policy agent, rego, conftest, kubernetes admission control, deny by default, mutation testing, devsecops, compliance as code, java, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/policy-as-code-explained.mp4` |
| Captions | `video/policy-as-code-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Infrastructure as Code Security in Java - With Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (92)
- [ ] tags are at most 500 characters in total — ok (193)
- [ ] description is at most 5,000 characters — ok (307)
- [ ] chapters start at 00:00 and there are at least three
