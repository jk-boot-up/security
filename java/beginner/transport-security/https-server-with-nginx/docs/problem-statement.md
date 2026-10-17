# Problem Statement

## The scenario

An online store wants its customers to reach it over HTTPS, and wants nginx at the front door to do the work, without trusting what a client says about itself.

## The naive version

Copy a configuration from the internet: it works in the browser, and it may still allow old protocols, lose HSTS on error pages, or believe a forged forwarded header.

## What this project must deliver

- A careful nginx configuration for TLS 1.3, HTTP/2, HSTS and a redirect, written out in `infra/nginx-careful.conf` and tested.
- A Java client that verifies the certificate chain and the host name and shows what it negotiated.
- Variants that show TLS 1.2, an impostor certificate, mutual TLS and a header trap.
- A check with `nginx -t` and a seven-point score for a configuration.
