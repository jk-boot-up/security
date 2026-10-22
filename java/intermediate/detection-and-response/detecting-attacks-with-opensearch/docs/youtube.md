# YouTube sheet — Detecting Attacks with OpenSearch

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Detecting Attacks with OpenSearch: Queries, Monitors, Tuning and What a Query Cannot Say (OWASP A09)

## Description

A real OpenSearch 3.9 with its alerting plugin in Docker, loaded with the 772 events of one synthetic Monday that hide four attacks: brute force, spraying, impossible travel and a privilege change. The detections written as queries (a filter, a terms aggregation, a date histogram and a threshold), tuned one threshold at a time and scored against the truth, then saved as bucket-level and query-level monitors and run once with dryrun; impossible travel done in code from the events read back; a mapping mistake that stops a rule, and an attack that falls between two fixed time buckets.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/intermediate/detection-and-response/detecting-attacks-with-opensearch

Chapters:

```
00:00 Introduction
01:53 The Scenario
02:14 Act One — One Monday of logs, in OpenSearch
02:31 Act Two — A first version of each rule, as a query
02:49 Act Three — Tuning one threshold
03:04 Act Four — The tuned set as monitors of the alerting plugin
03:24 Act Five — The bill
03:43 What It Costs
04:09 Who Does What
04:34 Where You Have Seen It
04:50 How To Use It
05:13 Thanks for Watching
```

## Tags

opensearch, detection, alerting, siem, brute force, password spraying, impossible travel, threat detection, owasp top 10, a09, java

## Files

| What | File |
| --- | --- |
| Video | `video/detecting-attacks-with-opensearch-explained.mp4` |
| Captions | `video/detecting-attacks-with-opensearch-explained.srt` |
| Thumbnail | `docs/thumbnail.png` (1280×720) |

## Settings

Public with instant Premiere; not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Security Foundations in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (100)
- [ ] tags are at most 500 characters in total — ok (121)
- [ ] description is at most 5,000 characters — ok (588)
- [ ] chapters start at 00:00 and there are at least three
