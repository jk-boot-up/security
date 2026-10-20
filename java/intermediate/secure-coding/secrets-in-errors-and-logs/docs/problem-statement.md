# Problem Statement

## The scenario

An online shop's checkout calls a database that is down. The team wants to find the cause quickly, and customers see an error page.

## The naive version

Show the error and log everything: the more detail, the easier the debugging, and only our own people read the logs.

## What this project must deliver

- A naive checkout and logger, and a fixed checkout and logger, with a fake driver error that contains a password.
- A redactor with Luhn card detection, key-name masking, bearer and JWT detection and URL passwords.
- A `Secret` wrapper that prints as redacted.
- Log injection, shown and fixed, and canary tests that look for known fake secrets in responses and logs.
