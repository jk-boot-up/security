# YouTube sheet — DNS Basics with CoreDNS

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

DNS Basics with CoreDNS (UDP, TCP, NXDOMAIN, Cache, Wrong Records)

## Description

DNS basics with CoreDNS, in Java: run a real DNS server in a container and ask it questions with our own code. Explained with an online store, like a phone book that prints whatever the last editor wrote. We query the server over UDP and TCP, see a name that does not exist, a zone it refuses to answer, a rewrite rule, the cache and the countdown of the time to live, and a zone with a wrong record that the server serves exactly like the right one, and finish with what really protects names: control of the zone file and DNSSEC.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/beginner/networking/dns-basics-with-coredns

Chapters:

```
00:00 Introduction
01:04 The Scenario
01:22 The Words
01:48 Act One — A real DNS server, in a container
02:15 Act Two — The same question over UDP and over TCP
02:46 Act Three — Names that do not exist, names it will not answer, and a rewrite
03:16 Act Four — The cache and the countdown of the time to live
03:43 Act Five — A zone with a wrong record
04:32 What Protects a Name
05:00 Who Does What
05:28 Where You Have Seen It
05:54 How To Use It
06:15 Thanks for Watching
```

## Tags

dns, coredns, docker, nxdomain, dns cache, ttl, zone file, java, cybersecurity for beginners

## Files

| What | File |
| --- | --- |
| Video | `video/dns-basics-with-coredns-explained.mp4` |
| Captions | `video/dns-basics-with-coredns-explained.srt` |
| Thumbnail | `docs/thumbnail.png` (1280×720) |

## Settings

Public with instant Premiere; not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Security Foundations in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (66)
- [ ] tags are at most 500 characters in total — ok (84)
- [ ] description is at most 5,000 characters — ok (531)
- [ ] chapters start at 00:00 and there are at least three
