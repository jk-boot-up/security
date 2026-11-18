# YouTube sheet — Kubernetes Network Policies

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Kubernetes Network Policies: Default Deny and Measuring Every Pair of Pods

## Description

Kubernetes network policies for an online store on a real kind cluster: default deny, one allow for each line of the design, measuring all 20 pairs of pods against the design, three mistakes the API server accepts, egress and DNS, and labels as the credential.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/advanced/kubernetes-and-container-security/kubernetes-network-policies

Chapters:

```
00:00 Introduction
00:53 The Scenario
01:27 The Words
02:06 What the Letters Stand For
03:14 What the Letters Stand For (continued)
03:22 What one allow says
04:17 No policy: every pod reaches every other pod
05:15 Act One — A flat network
05:30 Act One — A flat network
05:45 Close everything, then open the design
06:32 Act Two — Default deny, then the design
06:45 Act Two — Default deny, then the design
06:57 Act Two — Default deny, then the design
07:11 Act Two — Default deny, then the design
07:31 Three valid policies, three wrong grids
08:40 Act Three — Mistakes the API server accepts
08:58 Act Three — Mistakes the API server accepts
09:12 Act Three — Mistakes the API server accepts
09:30 Denying egress also stops the name lookup
10:31 Act Four — Egress and DNS
10:55 Act Four — Egress and DNS
11:14 Change a label, change the access
12:05 Act Five — The label is the credential
12:27 Act Five — The label is the credential
12:40 What It Costs
13:15 Where You Have Seen It
13:47 How To Use It
14:18 The Java Classes
14:31 Policies — a default deny: every pod, no rule
14:43 Policies — an allow: both selectors in one element
14:59 Lab — every pod tries every other pod
15:15 Intent — unwanted and missing
15:29 Cluster — curl inside a pod
15:43 Report — reachable = wanted + unwanted
15:58 Thanks for Watching
```

## Tags

kubernetes network policies, default deny, microsegmentation, kind, kindnet, dns egress, zero trust, kubernetes security, devsecops, java, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/kubernetes-network-policies-explained.mp4` |
| Captions | `video/kubernetes-network-policies-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Kubernetes and Container Security in Java - With Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (74)
- [ ] tags are at most 500 characters in total — ok (179)
- [ ] description is at most 5,000 characters — ok (260)
- [ ] chapters start at 00:00 and there are at least three
