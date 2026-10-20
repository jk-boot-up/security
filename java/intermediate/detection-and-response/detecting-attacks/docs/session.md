# Session Guide — Detecting Attacks in Logs

## Learning Objectives

By the end of the session you can:

- explain what each rule assumes about an attacker
- say why password spraying escapes a per-account rule
- choose a threshold by looking at the normal day and the attack
- name the context that removes false positives better than thresholds

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: One Monday of logs | 8 min |
| 0:18 | Act 2: A first version of each rule | 8 min |
| 0:26 | Act 3: Tuning one threshold | 8 min |
| 0:34 | Act 4: The tuned set, and what it tells the analyst | 8 min |
| 0:42 | Exercises | 18 min |

## Walkthrough

Run act two and ask which alerts a person would actually read. In act three, ask what the right threshold would be if the attacker went ten times slower. In act four, ask whether the two false alerts should be removed from the rule or left in.

## Exercises

1. Add a rule for a login from a country the account has never used, and measure it on the day.
2. Add a low-and-slow brute-force variant with a one-hour window, and see what it costs in false positives.
3. Add an off-hours admin login rule, and decide how to treat the break-glass account.
4. Write the spraying rule as a query over a log store with a group-by.
