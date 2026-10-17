# Problem Statement

## The scenario

An online store runs on a Spring Boot application that must serve its customers over HTTPS, send them away from plain HTTP, renew its certificate without downtime, and accept an internal service only if it shows a certificate.

## The naive version

Set a few TLS properties, see the padlock in a browser, and stop. The padlock says the connection is encrypted, not that the name is right, the old versions are off, the cookie is safe, or the certificate will be renewed.

## What this project must deliver

- TLS 1.3 only, from an SSL bundle named in properties.
- Clients built from a bundle that verify the certificate and the host name, and refuse everything else.
- A redirect from the plain port, HSTS, and a secure session cookie.
- A certificate renewed while the server runs, shown by fingerprint.
- Mutual TLS, with a client that has a certificate, one that has none, and one with a certificate signed by nobody.
- A list of what the framework leaves to you.
