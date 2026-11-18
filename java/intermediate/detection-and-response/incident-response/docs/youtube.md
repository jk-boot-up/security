# YouTube sheet — Incident Response: Prepare, Contain, Eradicate, Recover, Learn

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Incident Response: One Breach, Three Responses (NIST 800-61, Java)

## Description

Incident response in Java: one simulated breach of an online shop (a flaw in the web server, the customer table copied, three ways back in) replayed three times with different preparation and a different response, following the NIST SP 800-61 phases: prepare, detect and analyse, contain without destroying evidence, eradicate every way back in, recover and watch, and a review that compares time to detect, data lost, evidence and recurrence.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/intermediate/detection-and-response/incident-response

Chapters:

```
00:00 Introduction
00:52 The Scenario
01:24 The Words
02:07 What the Letters Stand For
03:00 Five ways back in, and what closes each
03:57 The attacker's script, and the rows it copies
05:03 Act One — The attack
05:23 Act One — The attack
05:48 What was prepared, and when it is found
06:53 Act Two — What was prepared
07:12 Response A: before, the five steps, and after
08:09 Act Three — Response A: nothing prepared, and a panicked fix
08:29 Act Three — Response A: nothing prepared, and a panicked fix
08:55 Response B: before, the five steps, and after
10:01 Act Four — Response B: an alert, but no plan
10:20 Act Four — Response B: an alert, but no plan
10:47 Response C: before, the eleven steps, and after
11:51 Act Five — Response C: the playbook
12:11 Act Five — Response C: the playbook
12:40 The review: the same breach, three responses
13:47 Act Six — The review
13:58 Act Six — The review
14:14 What It Costs
14:48 Where You Have Seen It
15:25 How To Use It
16:00 The Java Classes
16:14 Preparation — the checklist
16:29 World — the copying goes on until it is cut
16:42 World — the ways back in that are left
16:53 Responder — isolate: memory kept, route cut
17:06 Responder — reboot: evidence lost, persistence kept
17:16 Responder — you cannot disable what you do not know
17:25 Playbooks — the playbook's order
17:40 Outcome — recovered means nothing left open
17:51 Thanks for Watching
```

## Tags

incident response, nist 800-61, containment, eradication, forensics, post-incident review, java, application security, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/incident-response-explained.mp4` |
| Captions | `video/incident-response-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Detection and Response in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (66)
- [ ] tags are at most 500 characters in total — ok (161)
- [ ] description is at most 5,000 characters — ok (443)
- [ ] chapters start at 00:00 and there are at least three
