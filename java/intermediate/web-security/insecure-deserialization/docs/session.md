# Session Guide — Insecure Deserialization and Its Defences

## Learning Objectives

By the end of the session you can:

- explain why `readObject` can run code before the caller sees the object
- write an allow-list `ObjectInputFilter` with limits
- explain what a filter does not stop
- describe a data-only, signed alternative and why the server should own prices

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: A cart in a cookie | 8 min |
| 0:18 | Act 2: The cookie decides what the server builds | 8 min |
| 0:26 | Act 3: An allow-list filter names the classes a cart is made of | 8 min |
| 0:34 | Act 4: Data only: names and quantities, signed | 8 min |
| 0:42 | Exercises | 18 min |

## Walkthrough

Run act two and ask when the marker file was deleted, before or after the server checked the type. In act three, ask what the filter looked at. In act four, ask where the prices come from now.

## Exercises

1. Add a gift note field to the cart and update the allow-list, then watch an old cookie fail.
2. Add a version number to the signed cookie and rotate the key without logging everyone out.
3. Use a session id in the cookie and keep the cart in a map on the server, then compare what can go wrong.
4. Lower `maxdepth` until an honest cart is refused, and explain what you learned.
