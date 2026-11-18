# YouTube sheet — Detection as Code

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Detection as Code: Sigma Rules, ATT&CK Coverage and False-Positive Budgets

## Description

Detection engineering for a bank: Sigma rules tagged with MITRE ATT&CK, a rule's condition worked out on three events, precision and recall on a labelled week of logs, false-alarm budgets, rule tests and mutation testing, an ATT&CK coverage matrix, and pySigma's queries checked in OpenSearch.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/advanced/detection-engineering/detection-as-code

Chapters:

```
00:00 Introduction
00:45 The Scenario
01:15 The Words
01:58 What the Letters Stand For
02:58 What the Letters Stand For (continued)
03:10 What a Sigma rule is made of
04:10 Each selection answered, then the condition
05:33 Act One — One rule against three events
05:54 Act One — One rule against three events
06:10 Act One — One rule against three events
06:25 What each rule caught, missed and raised by mistake
07:34 Act Two — A week of logs, each rule scored
07:54 Act Two — A week of logs, each rule scored
08:12 Act Two — A week of logs, each rule scored
08:24 False alarms a day against a budget, and one filter
09:29 Act Three — False-alarm budgets and tuning
09:45 Act Three — False-alarm budgets and tuning
10:08 Act Three — False-alarm budgets and tuning
10:34 Every change to the rule makes a test fail
11:44 Act Four — Tests and mutants
12:00 Act Four — Tests and mutants
12:18 Twenty techniques, coloured by what the attacks showed
13:24 Act Five — ATT&CK coverage
13:38 Act Five — ATT&CK coverage
13:52 Act Five — ATT&CK coverage
14:03 The same rule in two engines
15:05 Act Six — pySigma and OpenSearch
15:17 Act Six — pySigma and OpenSearch
15:40 What It Costs
16:20 Where You Have Seen It
16:53 How To Use It
17:31 The Java Classes
17:44 Sigma — a selection: every field must match
17:54 Sigma — the condition: or, and, not
18:06 ConditionSteps — one selection, as its own rule
18:19 Engine — a correlation: N events in a window
18:35 Score — precision and the budget
18:47 Mutator and RuleTests — a mutant, and a passing case
19:04 Coverage — the status of a technique
19:15 Pysigma and OpenSearch — pinned, and compared
19:29 Thanks for Watching
```

## Tags

detection engineering, sigma, mitre attack, pysigma, opensearch, siem, false positives, mutation testing, threat detection, java, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/detection-as-code-explained.mp4` |
| Captions | `video/detection-as-code-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Detection Engineering in Java - With Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (74)
- [ ] tags are at most 500 characters in total — ok (170)
- [ ] description is at most 5,000 characters — ok (293)
- [ ] chapters start at 00:00 and there are at least three
