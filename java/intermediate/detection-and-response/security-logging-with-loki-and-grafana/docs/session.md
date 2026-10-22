# Session Guide — Security Logging with Loki and Grafana

## Learning Objectives

By the end of the session you can:

- push log lines to Loki with labels and a tenant
- write LogQL that answers an investigator's question
- choose what is a label and what stays in the line
- write an alert rule and prove that it fires

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: Shipping the events | 7 min |
| 0:17 | Act 2: Asking questions in LogQL | 7 min |
| 0:24 | Act 3: Labels and content | 7 min |
| 0:31 | Act 4: Alerting | 7 min |
| 0:38 | Act 5: The bill | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Run act two and read the story from one address. In act three compare the number of streams. In act four find the rule that never fires and say why.

## Exercises

1. Add a rule for a login success from an address that failed three times before.
2. Ship the same events from a Logback JSON appender instead of a push.
3. Add a third tenant and give it a rule of its own.
4. Make Grafana show the failures per address as a table.
