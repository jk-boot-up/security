# Session Guide — Cross-Site Scripting: Output Encoding and CSP

## Learning Objectives

By the end of the session you can:

- explain why text from a user can become part of a page
- choose the right encoding for text between tags, in an attribute, in a URL and in a script
- explain why input filtering is not a fix for cross-site scripting
- write a strict, nonce-based Content-Security-Policy and say what it does and does not stop

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: A review that changes the page | 7 min |
| 0:17 | Act 2: Encoding for the place the text goes | 7 min |
| 0:24 | Act 3: Why filtering input is not the fix | 7 min |
| 0:31 | Act 4: A strict Content-Security-Policy, with a fresh nonce for every page | 7 min |
| 0:38 | Act 5: The bill | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Start with the notice board. Run the demo one act at a time. After act one, ask what else a script running as the shop could do. In act two, ask the group to pick the encoder before
showing it. In act three, ask what the filter would need to know to work, and lead to the answer that only the place the text is written knows. In act four, ask what happens if
the nonce is cached with the page.

## Exercises

1. Add a fifth context, a CSS value in a style attribute, and decide whether to encode it or refuse it.
2. Make the review page show a star rating from a number, and decide which encoder, if any, it needs.
3. Change the policy to allow one script from a fixed address, and check the evaluator still refuses scripts without the nonce.
4. Find every place in the page where untrusted text is written, and write a test that fails if one is not encoded.
