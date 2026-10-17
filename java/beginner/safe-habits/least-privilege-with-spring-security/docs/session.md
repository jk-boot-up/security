# Session Guide — Least Privilege with Spring Security

## Learning Objectives

By the end of the session you can:

- explain why the last request rule should be deny
- choose between `@PreAuthorize` and `@PostAuthorize`
- put separation of duties where a role cannot bypass it
- compute what a stolen credential can reach
- describe an access grant that expires by itself

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: Deny by default: every path needs a rule | 7 min |
| 0:17 | Act 2: Method security: the rule sits next to the code | 7 min |
| 0:24 | Act 3: Separation of duties: two people for a refund | 7 min |
| 0:31 | Act 4: A role hierarchy, and what a stolen credential can reach | 7 min |
| 0:38 | Act 5: Access that expires by itself | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Start with the hotel and the master key. Run the demo one act at a time. After act one, ask which endpoints in a real application have no rule. After act two, ask what could go wrong with a post-authorized method that sends an email.
In act four, ask the group which account in their own system is the dave of the story.

## Exercises

1. Add a refunds:cancel action, a rule for it, and a test that fails if the rule is missing.
2. Change the last rule to `authenticated()` and find the paths that become open to everyone with a login.
3. Add an account for the warehouse export and print its blast radius.
4. Store grants in a database table and keep the same custom authorization manager.
