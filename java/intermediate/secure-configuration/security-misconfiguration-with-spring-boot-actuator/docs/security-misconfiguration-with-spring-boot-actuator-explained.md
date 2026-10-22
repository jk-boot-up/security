# Security Misconfiguration with Spring Boot Actuator, Explained

## The idea in one sentence

Actuator is a window into the running store, so the settings that decide who can look through it, and at what, are the security of the store.

## The 5 acts

### 1. Expose everything

The store starts with `include=*`, values shown, health details on, and the heap dump and shutdown switches turned on. The list at `/actuator` offers env, heapdump, mappings and shutdown, and the customers' port and the operations port are the same port. `/actuator/env` shows the payment key and the database password. `/actuator/mappings` lists `/internal/refunds`. The health page shows its components. The heap dump downloads with status 200 and contains the payment key. A post to the loggers endpoint changes a level with no login. The checker scores the store 0 of 8, and a post to `/actuator/shutdown` returns 200 and the store stops answering.

### 2. What Spring Boot already does

With no settings at all the list offers `health` only, `/actuator/env` is 404 and health shows a status, not components. With `include=*` and nothing else the payment key is shown as `******`, but the property names and the route map are still open, and `/actuator/heapdump` and `POST /actuator/shutdown` answer 404 until their access is set. With one more line, heapdump access unrestricted, the key is masked in the environment page and yet the dump contains it: masking hides values, not memory.

### 3. Hardened: another port, a short list, rules

The hardened store puts operations on a different port from the customers. `GET /products` works on the customers' port, and `/actuator/health` there is refused whether or not the caller has a login. On the operations port health answers 200 with status UP and no components, and env, heapdump, mappings, beans and shutdown are refused even for an operator. The checker scores the store 8 of 8.

### 4. Who may read, and who may change

`GET /actuator/loggers/ROOT` answers 401 with no login, 403 for a customer and 200 for an operator. Health shows its components to the operator and not to the customer. The operator posts DEBUG to the loggers endpoint and gets 405, and the level is still OFF, because `management.endpoint.loggers.access=read-only` removes the write operation even for the role that may read.

### 5. The bill

A developer adds `include=*` to the hardened store to debug something. The checker scores 7 of 8: the route map is now readable by an operator. A stranger asking for `/actuator/env` still gets 401, because the port, the list, the rules and the access levels are four separate settings and one of them failed while the others held. The same checks belong in CI on every change, because a dependency can add endpoints and a property can switch them on. A passing score means these 8 checks pass, not that the store is secure.

## The verdict

Never expose everything on a port that customers can reach. Put operations on their own port, list the endpoints you need, require a role for all but health, show details only to that role, make read-only what must not change, leave the heap dump and shutdown off, and run the checker on every change.

## How to recognise this in code you did not write

- `exposure.include=*` in a copied tutorial.
- Health details shown to everyone because a load balancer needed them.
- A heap dump sent to a support team.
- A `management.endpoint.*.access=unrestricted` line with no comment.

## Where you have already met this

- `management.endpoints.web.exposure.include=*` in a copied tutorial.
- `/actuator/health` used as a load balancer check, with details switched on.
- A heap dump attached to a support ticket.
- An internal URL that was never meant to be internal, found by a scanner.
