# Session Guide — One-Time Codes with the java-otp Library

## Learning Objectives

By the end of the session you can:

- say what java-otp does and what it does not do
- explain why the library's check is too strict for a bank, and how the bank widens it
- read an otpauth link and name its parts
- list what a bank must still write when it uses a one-time-code library

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: The library makes the same codes as the hand-written version | 7 min |
| 0:17 | Act 2: What the library checks, and what it leaves to the bank | 7 min |
| 0:24 | Act 3: Other hashes and longer codes | 7 min |
| 0:31 | Act 4: Giving the secret to the phone | 7 min |
| 0:38 | Act 5: The bill: what the library does not do | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Run the plain project first, then this one, and compare the first act: the same codes. Then read `OtpService` and find the single line
that calls the library, and the lines around it that the bank wrote. In act five, ask the group to list everything the bank must still do, and compare with
the library's documentation.

## Exercises

1. Change the library's step to 60 seconds. Which numbers in the demo change?
2. Write a test that fails if `OtpService` accepts a code twice with replay protection on.
3. Build the otpauth link for SHA-256 and 8 digits, and say which apps may not accept it.
4. Add a limit of 5 wrong codes per customer before further codes are refused.
