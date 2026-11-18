# YouTube sheet — Path Traversal, File Upload and Open Redirects

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Path Traversal, File Upload and Open Redirects in Java

## Description

Path traversal, unsafe file upload and open redirects, in Java. Three everyday features take a name or a link from a customer: download an invoice, upload a product photo, and return to a page after signing in. A file name with two dots walks out of its folder and reads the database settings, an uploaded file keeps the name and contents the customer chose, and a return address can send a signed-in customer to a look-alike site. Explained with an online store, like a cloakroom ticket that someone has rewritten to point at the manager's office. We watch a text check on the path fail against a folder with a similar name and a symbolic link, then compare paths as paths and follow links first, let the server decide each upload's name and type from its bytes and keep it outside the web root, and send customers only to paths on our own site.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/intermediate/web-security/path-traversal-and-file-upload

Chapters:

```
00:00 Introduction
00:52 The Scenario
01:20 The Words
02:02 What the Letters Stand For
03:07 What the Letters Stand For (continued)
03:29 A name that walks out of its folder
04:29 The folders, one name, one path
05:27 Act One — An honest name downloads one invoice
05:39 Act One — An honest name downloads one invoice
05:53 Two dots cancel the folder before them
06:52 Act Two — A name with .. climbs out of the folder
07:10 Act Two — A name with .. climbs out of the folder
07:20 Same letters, different folder
08:22 Act Three — A text check is not a path check
08:36 Act Three — A text check is not a path check
08:52 Six requests, four layers
10:01 Act Four — The fixed version checks in four layers
10:10 Act Four — The fixed version checks in four layers
10:26 Act Four — The fixed version checks in four layers
10:45 Three uploads, saved as they came
11:47 Act Five — The naive upload trusts the customer's name
12:07 The bytes and the size decide; the name does not
13:18 Act Six — The fixed upload decides for itself
13:34 Act Six — The fixed upload decides for itself
13:48 Act Six — The fixed upload decides for itself
13:57 Where each link sends the customer
15:05 Act Seven — Return links stay on our own site
15:17 Act Seven — Return links stay on our own site
15:32 What It Costs
16:12 Where You Have Seen It
16:42 How To Use It
17:19 The Java Classes
17:33 NaiveDownloads — resolve, and read
17:44 PrefixDownloads — a text prefix
17:56 SafeDownloads — decode, shape, path, real path
18:16 NaiveUploads — the customer's name
18:29 Sniffer — magic bytes
18:49 SafeUploads — the server's own name
19:06 ReturnLink — only a path on our site
19:24 Thanks for Watching
```

## Tags

path traversal, file upload, open redirect, owasp top 10, java, application security, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/path-traversal-and-file-upload-explained.mp4` |
| Captions | `video/path-traversal-and-file-upload-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Web Security in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (54)
- [ ] tags are at most 500 characters in total — ok (130)
- [ ] description is at most 5,000 characters — ok (846)
- [ ] chapters start at 00:00 and there are at least three
