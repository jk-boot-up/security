# Session Guide — One-Time Codes (TOTP)

## Learning Objectives

By the end of the session you can:

- explain what a second factor adds to a password
- explain how the phone and the bank get the same code without talking
- say why the bank accepts a window of steps, and what a wider window costs
- name three things a TOTP code cannot protect against

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: A password alone | 5 min |
| 0:15 | Act 2: Enrolling: one secret, shared once | 5 min |
| 0:20 | Act 3: How the code is made, step by step | 5 min |
| 0:25 | Act 4: Logging in, and clocks that disagree | 5 min |
| 0:30 | Act 5: The same code twice | 5 min |
| 0:35 | Act 6: The bill: what a code cannot do | 5 min |
| 0:40 | Exercises | 20 min |

## Walkthrough

Start with the two-key safe-deposit box. Run the demo one act at a time. After acts two and three, ask the group how the phone and the bank can agree on a code
without a message between them, and let them name the shared secret and the clock. After act four, ask what the bank loses if it widens the window to 5. In
act six, ask what a hardware security key does that a code cannot, and let the group discover that it checks which site it is talking to.

## Exercises

1. Change the window to 0 and rerun the clock tests. Which of them change?
2. Change the step from 30 seconds to 60. Which code values in the demo change?
3. Use 8 digits instead of 6 and check the codes against the RFC 6238 table.
4. Add a limit of 5 wrong codes before the account locks, and decide what the unlock should need.
