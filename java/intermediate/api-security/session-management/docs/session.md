# Session Guide — Session Management: Ids, Rotation, Timeouts and Sign-Out

## Learning Objectives

By the end of the session you can:

- explain why a session id is as good as a password while it lives
- generate and store session ids safely
- explain session fixation and why a new id at sign-in stops it
- choose idle and absolute timeouts and decide when to ask for the password again
- sign out on the server, everywhere, and after a password change

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: A session id: random, and never stored as itself | 7 min |
| 0:17 | Act 2: A new id at every sign-in and step-up | 7 min |
| 0:24 | Act 3: How long a session lives | 7 min |
| 0:31 | Act 4: Signing out, everywhere, and the cookie | 7 min |
| 0:38 | Act 5: The bill | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Start with the cloakroom ticket. In act one, ask what a stolen copy of the session store would give an attacker. In act two, ask why the planted id worked at all.
In act three, ask which timeout matters more for a bank and which for a news site. In act four, ask what happens to a copied cookie after sign-out. In act five, ask
where the session store should live when there are ten servers.

## Exercises

1. Make the idle timeout shorter for sessions that have made a payment.
2. Add a list of Ana's sessions with the time each was last used, and sign out one of them.
3. Store the time of the last password change in the session, and refuse sessions older than it.
4. Move the session store into a separate class with a time to live per entry, as Redis would keep it.
