# Problem Statement

## The scenario

An online store logs its checkout so that its team can find faults, and the logs are copied to places that many people can read.

## The naive version

Log whatever is at hand and trust the file's permissions.

## What this project must deliver

- Logback and the Logstash encoder configured in code.
- A masking layout, an exception converter and a Luhn check.
- Types that cannot print a secret.
- JSON events with masking by path and by shape.
