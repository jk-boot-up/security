# Session Guide — The CIA Triad

## Learning Objectives

By the end of the session you can:

- state the three promises of the CIA triad in your own words
- name which promise fails in a given incident
- match a simple control to the promise it protects
- explain why the three promises can pull against each other

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: The first version, with no controls | 7 min |
| 0:17 | Act 2: Confidentiality: each role sees what it needs | 7 min |
| 0:24 | Act 3: Integrity: a change behind the application is caught | 7 min |
| 0:31 | Act 4: Availability: one client cannot use up the server | 7 min |
| 0:38 | Act 5: The bill: strict controls get in the way of emergencies | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Start with the vault analogy and ask the group to name one thing that could go
wrong with a vault for each of the three promises. Then run the demo one act at
a time. After each act, pause and ask which promise just failed or was restored,
before reading the label. In act five, ask the group whether they would allow
the emergency exception, and what they would want to know about each use.

## Exercises

1. Add a third role, WAREHOUSE, that may see the customer, the item and the address but not the amount. Which test must change?
2. Change the per-client limit to 1. Who is affected by that, besides the bot?
3. Make the seal also cover the audit log entries. What do you have to decide about the key?
4. Write down one incident from a news story and decide which of the three promises it broke.
