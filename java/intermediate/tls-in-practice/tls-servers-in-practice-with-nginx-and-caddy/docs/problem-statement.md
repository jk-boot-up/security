# Problem Statement

## The scenario

An online store serves its pages itself and wants them only over https, with certificates from its own authority.

## The naive version

Copy a configuration from a blog post and trust that it does what it says.

## What this project must deliver

- A real nginx and a real Caddy.
- A client that shows what a browser would see.
- Protocol versions, HSTS and redirects, one line at a time.
- Renewal and stapling, and what they need.
