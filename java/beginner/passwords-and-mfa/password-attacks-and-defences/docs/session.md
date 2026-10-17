# Session Guide — Password Attacks and Defences

## Learning Objectives

By the end of the session you can:

- name the three ways of guessing passwords online and tell them apart
- say which defence stops which attack, and which it misses
- explain how a lockout can be turned into a denial of service
- explain why an error message can leak information

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: No defences | 7 min |
| 0:17 | Act 2: Lock an account after 5 wrong passwords, for 10 ticks | 7 min |
| 0:24 | Act 3: Allow each address 5 attempts in every 10 ticks | 7 min |
| 0:31 | Act 4: Credential stuffing: passwords leaked from another site | 7 min |
| 0:38 | Act 5: The bill: a lock can be used against the customer, and messages can leak | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Start with the night guard analogy. Run the demo one act at a time. After act two, ask what kind of attack the lockout does not
stop, and let the group name spraying before it is shown. After act three, ask what an attacker with a thousand addresses would do. In act four, ask what the
bank could do about customers who reuse passwords, and what it costs them. In act five, ask whether the group would rather lock out a real customer
or let a guesser try a few more times, and why the answer depends on the bank.

## Exercises

1. Raise the lockout to 10 failures. Which acts change, and which do not?
2. Add a fourth spray password, 12345678. Does the spray take more accounts, and how many ticks does it need?
3. Make the rate limit apply per username as well as per address. Which attack does that slow most?
4. Add a delay that doubles after each failure instead of a fixed lock, and compare the dictionary attack's time.
