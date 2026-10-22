# Problem Statement

## The scenario

A bank wants every deployment checked against its rules, in the build and again in the cluster.

## The naive version

Write the rules on a wiki page and ask reviewers to remember them.

## What this project must deliver

- A deny-by-default policy in Rego.
- A table of requests it must decide as a reviewer expects.
- Tests for the policy and mutation testing of those tests.
- An AdmissionReview and Conftest on files, and a warning-first rollout.
