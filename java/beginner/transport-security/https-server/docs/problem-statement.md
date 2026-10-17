# Problem Statement

## The scenario

An online store must serve customers only over HTTPS, keep their login cookie private, and let them tell the real shop from an impostor, while some visitors still arrive by typing an old plain-HTTP address.

## The naive version

Install a certificate and assume the job is done. The padlock appears, and the plain port, the cookie flags and the old TLS versions are left as they were.

## What this project must deliver

- A real HTTPS server and a plain HTTP server on this machine, started and stopped by the code.
- A client that verifies the certificate and the host name, and refuses impostors, wrong names and downgrades.
- A redirect with HSTS, and cookies that stay on HTTPS, and a demonstration of a leak.
- A five-point checklist run from outside against a careful and a careless shop.
- The same output on every run, with random ports never printed.
