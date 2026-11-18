# YouTube sheet — TLS Servers in Practice with nginx and Caddy

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

TLS Servers in Practice with nginx and Caddy: Protocols, HSTS, Redirects, Renewal (OWASP A02, A04)

## Description

A real nginx 1.31 and a real Caddy 2.11 on loopback ports in front of the same shop: a certificate from a local root made with the step command, a client that needs the root and the right name, the redirect from http, the Server header and server_tokens, TLS 1.2 against a TLS 1.3 only setting, HSTS and the add_header inheritance trap, Caddy's own local authority with 12-hour certificates and automatic redirects, OCSP stapling that needs a responder, and nginx serving the old certificate until it is reloaded.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/intermediate/tls-in-practice/tls-servers-in-practice-with-nginx-and-caddy

Chapters:

```
00:00 Introduction
00:55 The Scenario
01:25 The Words
02:03 What the Letters Stand For
03:10 What the Letters Stand For (continued)
03:31 The same choices on two servers
04:20 What the HSTS header promises
05:07 Three clients, two checks, one certificate
06:20 Act One — nginx with a certificate from the local authority
06:38 Act One — nginx with a certificate from the local authority
06:55 Act One — nginx with a certificate from the local authority
07:14 The highest version both sides speak
08:09 Act Two — Which protocol versions it speaks
08:27 Act Two — Which protocol versions it speaks
08:41 A location's own add_header replaces the server's
09:35 Act Three — HSTS, and the add_header trap
10:06 Act Three — HSTS, and the add_header trap
10:17 Act Three — HSTS, and the add_header trap
10:29 A short Caddyfile, a whole chain behind it
11:30 Act Four — Caddy makes its own certificates
11:46 Act Four — Caddy makes its own certificates
12:08 No responder, nothing stapled; no reload, the old certificate
13:02 Act Five — The bill
13:19 Act Five — The bill
13:30 What It Costs
14:05 Where You Have Seen It
14:40 How To Use It
15:16 The Java Classes
15:30 Https — server name and name check
15:50 ChainCheck — who signed it, and whom it names
16:08 Negotiation — the highest version in common
16:20 Headers — a location's own lines win
16:35 Nginx — one line per choice, and a reload
16:57 Caddy — only the site, and local certificates
17:11 Thanks for Watching
```

## Tags

nginx, caddy, tls, hsts, ocsp stapling, automatic https, tls 1.3, owasp top 10, a02, java, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/tls-servers-in-practice-with-nginx-and-caddy-explained.mp4` |
| Captions | `video/tls-servers-in-practice-with-nginx-and-caddy-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: TLS in Practice in Java - With Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — FIX (101)
- [ ] tags are at most 500 characters in total — ok (131)
- [ ] description is at most 5,000 characters — ok (513)
- [ ] chapters start at 00:00 and there are at least three
