# Session Guide — Network Isolation

## Learning Objectives

By the end of the session you can:

- measure what a service can reach
- explain why a network joins both directions
- build a layout from a table of allowed pairs
- find drift by comparing with the intent

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: One flat network | 7 min |
| 0:17 | Act 2: Three networks: front, back, management | 7 min |
| 0:24 | Act 3: The design as code, and a layout built from it | 7 min |
| 0:31 | Act 4: The edge: what is published, and what can leave | 7 min |
| 0:38 | Act 5: The bill | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Run act two and name the two pairs nobody asked for. In act three find the line that shows drift. In act four find the only service that can leave.

## Exercises

1. Add a metrics service that every service may talk to, and see what the layout becomes.
2. Close the front's way out and keep its published port.
3. Write the intent for a service with three replicas.
4. Make the measurement a test that fails when there is an extra pair.
