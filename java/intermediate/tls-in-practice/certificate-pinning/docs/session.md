# Session Guide — Certificate Pinning and What It Costs

## Learning Objectives

By the end of the session you can:

- explain what a pin adds to certificate validation and what it does not replace
- choose between pinning a leaf, an issuing CA and a backup key
- describe the rotation outage and how to plan for it
- explain why server-set pins were dropped by browsers

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: An app that trusts the device | 7 min |
| 0:17 | Act 2: An app that pins the server's key | 7 min |
| 0:24 | Act 3: The server renews with a new key | 7 min |
| 0:31 | Act 4: Pins chosen so that rotation does not break the app | 7 min |
| 0:38 | Act 5: Why browsers stopped letting servers set pins | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Run act two and ask who the pin protects against, and who it does not. In act three, ask what the bank could have done before the change. In act five, ask who controls the response that sets the pin.

## Exercises

1. Add a pin set with an enforcement date, and refuse the policy once the date has passed without an update.
2. Add a signed remote switch that turns enforcement off, and decide what an attacker can do with it.
3. Write the rotation procedure for the bank: which key is generated when, and which app release carries which pin.
4. Add Certificate Transparency checking as an alternative and compare the failure modes.
