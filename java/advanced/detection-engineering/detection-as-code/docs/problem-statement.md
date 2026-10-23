# Problem Statement

## The scenario

A bank wants to know whether its detections catch the attacks they are written for, how many false alarms they cost, and what they miss.

## The naive version

Edit rules in the console and judge them by how quiet they are.

## What this project must deliver

- Sigma rules with ATT&CK tags.
- A labelled week of logs.
- Precision, recall and a false-alarm budget for each rule.
- Rule tests, mutation tests and a coverage matrix.
- pySigma conversion checked against OpenSearch.
