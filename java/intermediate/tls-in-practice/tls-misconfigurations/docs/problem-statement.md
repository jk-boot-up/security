# Problem Statement

## The scenario

An online shop runs several HTTPS servers set up at different times by different people, and nobody knows which of them would pass a security review.

## The naive version

If the browser shows a padlock, TLS is fine.

## What this project must deliver

- A server profile model and four fixed servers.
- A checker with stable rule ids, severities, a score and a grade.
- Cipher suite classification by IANA name.
- Policies after the Mozilla profiles that fix protocols and suites, and an nginx fragment generator.
