# Problem Statement

## The scenario

An online store runs on a Spring Boot application. It must send the safe headers, keep the session cookie from leaking, refuse malformed requests and bound their size, without hand-written HTTP code.

## The naive version

Rely on the framework's defaults without looking at the real responses, and trust a forwarded header from anywhere.

## What this project must deliver

- Two real Spring Boot servers, bare and hardened, started on free ports.
- The five-header audit applied to their real responses: 0 of 5, 4 of 5 and 5 of 5.
- The session cookie judged before and after three properties.
- A header injection attempt and a malformed request refused by the container.
- A request size limit shown in effect.
