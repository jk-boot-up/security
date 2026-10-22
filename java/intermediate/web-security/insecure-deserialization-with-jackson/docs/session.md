# Session Guide — Insecure Deserialization with Jackson

## Learning Objectives

By the end of the session you can:

- explain how a type id in JSON lets the data choose a class
- say what Jackson 3's default validator protects and what it does not
- fix a polymorphic field with names or a validator
- keep prices and types out of client data

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: A cart in a cookie | 7 min |
| 0:17 | Act 2: The cookie decides which class is built | 7 min |
| 0:24 | Act 3: A list of kinds, or a list of classes | 7 min |
| 0:31 | Act 4: Data only, priced by the shop | 7 min |
| 0:38 | Act 5: Limits on the stream | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Run act two and ask what protected the Object field and why the note field was not protected. In act three ask what the forged price shows. In act four ask who decides the price.

## Exercises

1. Add a third kind of note and see which fix needs a change.
2. Write a test that fails if any annotation uses Id.CLASS on a field.
3. Turn on `MapperFeature` strictness options and see what changes.
4. Add a signature to the cookie and decide what it fixes and what it does not.
