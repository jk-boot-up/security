# Session Guide — HTTPS Server with nginx

## Learning Objectives

By the end of the session you can:

- explain what ending TLS at the front door means
- choose protocol versions for a site
- explain what HSTS does and what it cannot do on the first visit
- say why a proxy must set X-Forwarded-Proto itself
- run nginx -t before deploying a configuration

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: nginx ends the TLS for the shop | 7 min |
| 0:17 | Act 2: Which TLS versions the shop allows | 7 min |
| 0:24 | Act 3: The plain port, the redirect and HSTS | 7 min |
| 0:31 | Act 4: Who the client trusts, and who the server trusts | 7 min |
| 0:38 | Act 5: The proxy trap, a config check and a score | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Start with the receptionist. Run the demo one act at a time. After act three, ask what a visitor on a hotel network could do on the very first visit. In act four, ask why a name check matters when the chain is already trusted.
In act five, show the one-line difference between the trap and the safe configuration, and ask where else a service believes a header it should set itself.

## Exercises

1. Add `ssl_stapling on;` to the configuration and find out why nginx complains when it cannot reach the authority.
2. Make the plain port serve `/health` only on 127.0.0.1 and redirect everything else.
3. Change the Java client to resume a session and observe what `ssl_session_tickets off` changes.
4. Add a second server block for another host name and give it its own certificate.
