# Problem Statement

## The scenario

A small shop runs on Linux servers built from an image that has grown for years, and nobody can say which of its settings are intentional.

## The naive version

The image boots and the application works, so the host is fine.

## What this project must deliver

- A prepared image tree with real Unix permission bits and the usual mistakes.
- A checker for permissions, set-user-id files, accounts, SSH, kernel settings and listening ports, with a policy.
- A safe fixer for permission findings only.
- A report that separates fixable findings from decisions.
