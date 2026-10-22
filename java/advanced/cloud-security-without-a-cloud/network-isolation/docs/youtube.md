# YouTube sheet — Network Isolation

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Network Isolation: Measuring What a Taken-Over Service Can Reach (Docker Networks, Intent as Code)

## Description

Network segmentation measured on real Docker networks for an online store's six services: one flat network where a taken-over front reaches the database, the cache, the admin panel and the build server; three coarse networks; a layout generated from the design as code with one internal network for each allowed pair; drift found by comparing what can talk with what should; published ports; and which services can reach a server on the internet.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/advanced/cloud-security-without-a-cloud/network-isolation

Chapters:

```
00:00 Introduction
01:47 The Scenario
02:04 Act One — One flat network
02:24 Act Two — Three networks: front, back, management
02:40 Act Three — The design as code, and a layout built from it
03:01 Act Four — The edge: what is published, and what can leave
03:17 Act Five — The bill
03:38 What It Costs
04:00 Who Does What
04:17 Where You Have Seen It
04:37 How To Use It
04:58 Thanks for Watching
```

## Tags

network segmentation, network isolation, docker networks, lateral movement, blast radius, security groups, zero trust, cloud security, drift detection, java

## Files

| What | File |
| --- | --- |
| Video | `video/network-isolation-explained.mp4` |
| Captions | `video/network-isolation-explained.srt` |
| Thumbnail | `docs/thumbnail.png` (1280×720) |

## Settings

Public with instant Premiere; not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Security Foundations in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (98)
- [ ] tags are at most 500 characters in total — ok (147)
- [ ] description is at most 5,000 characters — ok (446)
- [ ] chapters start at 00:00 and there are at least three
