# Security Misconfiguration with Spring Boot Actuator, Explained

## The idea in one sentence

Actuator is a window into the running store, so the settings that decide who can look through it, and at what, are the security of the store.

## The 5 acts

### 1. Expose everything

The shipped profile has six settings from a tutorial: management.endpoints.web.exposure.include=*, show-values=ALWAYS for env and configprops, health show-details=always, and unrestricted access for shutdown and heapdump. Customers and operations share one port.
A stranger with no login sends seven requests. GET /actuator lists 14 endpoints, among them env, heapdump and shutdown. GET /actuator/env/shop.payment.api-key answers "value":"pay-live-4f9a1c77-demo-only", the live key, and shop.db.password answers "value":"pg-demo-passw0rd". GET /actuator/mappings lists /internal/refunds/{id}, a route no page links to. GET /actuator/health names its components: diskSpace, livenessState, ping, readinessState and ssl. GET /actuator/heapdump answers 200, and the payment key is in the dump. POST to the loggers endpoint answers 204, and the level is now DEBUG.
The checker's eight checks all fail: 0 of 8. Last, POST /actuator/shutdown answers 200 and the store stops answering.

### 2. What Spring Boot already does

With no actuator settings, GET /actuator offers health only, GET /actuator/env answers 404, and health shows a status with no components: {"groups":["liveness","readiness"],"status":"UP"}.
With include=* and nothing else, Spring Boot masks values: the payment key is "value":"******". But GET /actuator/mappings still lists /internal/refunds/{id}, a map for the next step. heapdump and shutdown answer 404 until someone sets their access.
One more line, management.endpoint.heapdump.access=unrestricted: the env page still shows ******, yet the heap dump answers 200 and the key is in it. Masking hides values, not memory.

### 3. Hardened: another port, a short list, rules

The hardened settings: management.server.port=0 and management.server.address=127.0.0.1, an operations port of its own; exposure.include=health,info,loggers; health show-details=when-authorized for the role OPS; loggers access=read-only. Customers and operations no longer share a port.
The rules in StoreSecurity, first match wins: health for everyone, the other exposed endpoints for the role OPS, /products and /orders/** for everyone, anything else denied.
On the customers' port, /products answers 200 and /actuator/health answers 401, and 403 even with the operator's login. On the operations port, health answers 200 with a status only. env, heapdump, mappings and shutdown answer 403 to the operator: they are not on the list, so no rule allows them. The checker scores 8 of 8.

### 4. Who may read, and who may change

GET /actuator/loggers/ROOT: no login gets 401, the customer ana gets 403, the operator ops gets 200 and {"configuredLevel":"OFF","effectiveLevel":"OFF"}.
GET /actuator/health: ana sees no components, a status only; ops sees diskSpace, livenessState, ping, readinessState and ssl.
The operator posts {"configuredLevel":"DEBUG"} to the loggers endpoint: 405, and the level is still OFF, because access is read-only.

### 5. The bill

A developer adds management.endpoints.web.exposure.include=* to the hardened store to debug something. The checker scores 7 of 8: route map hidden fails, GET /actuator/mappings as ops answers 200.
The login rules still held: GET /actuator/env without a login answers 401. That is why the port, the list, the rules and the access levels are four settings and four layers.
A dependency can add endpoints and a property can expose them, so the checks run in CI, on every change. Passing means these 8 checks pass, not that the store is safe.

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
