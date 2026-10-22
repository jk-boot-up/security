# Problem Statement

## The scenario

An online store accepts a checkout form from outside, and the same rules are needed at the page, the controller and the service.

## The naive version

Check a few fields in each place by hand, and trust the page for the rest.

## What this project must deliver

- Rules written once, as annotations on records.
- A service whose parameter is validated.
- Custom constraints, a rule across two fields and a group sequence.
- Limits, patterns and messages treated as part of the rules.
