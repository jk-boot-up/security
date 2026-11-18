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
| 0:10 | Act 1: One Monday of logs | 7 min |
| 0:17 | Act 2: Two attacks, worked out | 7 min |
| 0:24 | Act 3: A first version of each rule | 7 min |
| 0:31 | Act 4: Tuning one threshold | 7 min |
| 0:38 | Act 5: The tuned set, and what it tells the analyst | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

In act two, work out the travel speed for another pair of cities by hand. Run act three and ask which alerts a person would actually read. In act four, ask what the safe range would be if the attacker went ten times slower. In act five, ask whether the two false alerts should be removed from the rule or left in.

## Exercises

1. Add a rule for a login from a country the account has never used, and measure it on the day.
2. Add a low-and-slow brute-force variant with a one-hour window, and see what it costs in false positives.
3. Add an off-hours admin login rule, and decide how to treat the break-glass account.
4. Write the spraying rule as a query over a log store with a group-by.
