# Problem Statement

## The scenario

A mobile banking app talks to the bank's API, on devices whose trust stores the bank does not control.

## The naive version

Rely on ordinary TLS validation: a valid chain means we are talking to the bank.

## What this project must deliver

- Real certificate chains for a public hierarchy and for an interception proxy.
- A client that validates and then checks SPKI pins, with report-only mode.
- A calculation of the fleet lockout when the key changes.
- A model of server-set (HPKP-style) pins and the lockouts they cause.
