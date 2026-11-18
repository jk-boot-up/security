# YouTube sheet — Network Isolation

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Network Isolation in Java: Measuring What a Taken-Over Service Can Reach (Docker Networks)

## Description

Network segmentation measured on real Docker networks for an online store's six services: one flat network where a taken-over front reaches the database, the cache, the admin panel and the build server; three coarse networks; a layout generated from the design as code with one internal network for each allowed pair; drift found by comparing what can talk with what should; published ports; and which services can reach a server on the internet.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/advanced/cloud-security-without-a-cloud/network-isolation

Chapters:

```
00:00 Introduction
00:53 The Scenario
01:24 The Words
02:04 What the Letters Stand For
02:45 How many pairs one network makes
03:31 Taken over on a flat network, the front reaches everything
04:23 Act One — One flat network
04:42 Act One — One flat network
05:02 Three networks, and the pairs each one makes
05:57 Act Two — Three networks: front, back, management
06:13 Act Two — Three networks: front, back, management
06:34 One network for each allowed pair, and the drift
07:34 Act Three — The design as code, and a layout built from it
07:46 Act Three — The design as code, and a layout built from it
08:06 The way in and the way out
08:57 Act Four — The edge: what is published, and what can leave
09:12 Act Four — The edge: what is published, and what can leave
09:28 The pivots: services on more than one network
10:09 Act Five — The bill
10:36 What It Costs
11:10 Where You Have Seen It
11:39 How To Use It
12:11 The Java Classes
12:23 Layout — every two members of a network are a pair
12:37 Layout — n services make n x (n - 1) / 2 pairs
12:48 Intent — extra: reachable, and not wanted
13:00 Lab — an internal network has no way out
13:09 Lab — measure from inside a container
13:23 Thanks for Watching
```

## Tags

network segmentation, network isolation, docker networks, lateral movement, blast radius, security groups, zero trust, cloud security, drift detection, java, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/network-isolation-explained.mp4` |
| Captions | `video/network-isolation-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Cloud Security Without a Cloud in Java - With Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (90)
- [ ] tags are at most 500 characters in total — ok (198)
- [ ] description is at most 5,000 characters — ok (446)
- [ ] chapters start at 00:00 and there are at least three
