# YouTube sheet — TLS Servers in Practice with nginx and Caddy

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

TLS Servers in Practice with nginx and Caddy: Protocols, HSTS, Redirects and Renewal (OWASP A02, A04)

## Description

A real nginx 1.31 and a real Caddy 2.11 on loopback ports in front of the same shop: a certificate from a local root made with the step command, a client that needs the root and the right name, the redirect from http, the Server header and server_tokens, TLS 1.2 against a TLS 1.3 only setting, HSTS and the add_header inheritance trap, Caddy's own local authority with 12-hour certificates and automatic redirects, OCSP stapling that needs a responder, and nginx serving the old certificate until it is reloaded.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/intermediate/tls-in-practice/tls-servers-in-practice-with-nginx-and-caddy

Chapters:

```
00:00 Introduction
01:30 The Scenario
01:49 Act One — nginx with a certificate from the local authority
02:14 Act Two — Which protocol versions it speaks
02:34 Act Three — HSTS, and the add_header trap
02:56 Act Four — Caddy makes its own certificates
03:15 Act Five — The bill
03:34 What It Costs
03:57 Who Does What
04:17 Where You Have Seen It
04:32 How To Use It
04:55 Thanks for Watching
```

## Tags

nginx, caddy, tls, hsts, ocsp stapling, automatic https, tls 1.3, owasp top 10, a02, java

## Files

| What | File |
| --- | --- |
| Video | `video/tls-servers-in-practice-with-nginx-and-caddy-explained.mp4` |
| Captions | `video/tls-servers-in-practice-with-nginx-and-caddy-explained.srt` |
| Thumbnail | `docs/thumbnail.png` (1280×720) |

## Settings

Public with instant Premiere; not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Security Foundations in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — FIX (101)
- [ ] tags are at most 500 characters in total — ok (80)
- [ ] description is at most 5,000 characters — ok (513)
- [ ] chapters start at 00:00 and there are at least three
