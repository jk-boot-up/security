# Broken Access Control, Explained

## The idea in one sentence

Broken access control is a server that does what any signed-in caller asks, because the check for whose object it is or which role may act was missing, misplaced or only in the page; the fix is one policy that runs before every handler, refuses whatever it has no rule for, and checks ownership of every object.

## The 5 acts

### 1. Walking order numbers

Ana is signed in to the naive store. She changes the number in the address from 1000 to 1010, one request each. Seven numbers answer 404, and four answer 200: 1001 and 1003 are hers, 1002 is ben's and 1004 is cara's. Every order is one number away.
Ben's gift order has an id nobody could guess, c5e1f0a8-..., but ben shared the link, and ana opens it: 200. A hard-to-guess id is not a permission.
Signing in proved who ana is; nothing asked whether the order is hers. This is IDOR (CWE-639), and API1 in the OWASP API list.

### 2. Routes that nobody checks

The page ana sees shows only two links, orders and account; the admin links are hidden. Ana sends four requests by hand.
POST /admin/refunds answers 200: the role was checked only in the page. GET /admin/reports, signed out, answers 200: nobody links to it, but it is still a route. GET /admin/users answers 403, and DELETE /admin/users/ben answers 200: one method was checked, and the other forgotten.
The result: ben's account no longer exists. These are missing function-level checks (CWE-862), API5 in the OWASP API list.

### 3. Sideways and upwards

Sideways means the same role and someone else's data: ana posts a new address, 12 Palm Street, to ben's order 1002, and ben's order now ships there. Upwards means a higher role's function: ana posts to /admin/refunds, and the refund of 1002 is recorded.
Before the fix, nine attacks are tried against the naive store. Seven answer 200, through five of its nine routes. Two are refused with 403: listing the users, and cancelling ben's order, because DELETE /orders/{id} was the one route written carefully. The checks were not all wrong; each was written separately, and some were forgotten.

### 4. One choke point, deny by default

After the fix, every check is taken out of the handlers and put in one policy that runs before any handler. The same nine attacks are sent. Someone else's order answers 404, signed out answers 401, and the wrong role answers 403. Attacks that succeed: 0 of 9.
The six honest requests still work: ana reads and moves her own orders, sam from support reads ben's order, ben cancels his own order, and dave the admin reads the report and issues a refund. 6 of 6.
Someone else's order answers like a missing one, so the reply does not confirm that the order exists. Nine refusals are logged with who asked and why, for example DENY ana GET /orders/{id}: no order 1002 that belongs to ana, and no token appears in the log.

### 5. Keeping it right, and the bill

Today no route lacks a rule. A developer adds GET /admin/export and forgets its rule. The route inventory test reads every route, lists [GET /admin/export], and fails the build; until the route has a rule even dave the admin gets 403, denied by default.
Over real HTTP on this machine, the same rules answer 200 for ana's order 1001, 404 for ben's order 1002, 403 for DELETE /admin/users/ben, and 401 for the report signed out.
The bill: the naive store kept its checks in 9 handlers, nine places to forget one; the guarded store keeps them in one table that a test can read. Every object check needs a lookup of who owns the object, and every refusal is a log line to keep and watch.

## The verdict

Deny by default. Put every rule in one place that runs before any handler, and test that every route has one. Check ownership of each object on the server, every time, and prefer queries that can only return the caller's own data.
Never rely on hidden links, unguessable ids or the method being one the page does not use. Log refusals, without secrets.

## How to recognise this in code you did not write

- `anyRequest().denyAll()` or `anyRequest().authenticated()` at the end of a security configuration.
- `@PreAuthorize("@orders.isOwner(#id, authentication)")` on a method.
- Queries such as `findByIdAndOwner(id, user)` instead of `findById(id)`.
- Pen-test findings titled IDOR, BOLA or BFLA.

## Where you have already met this

- Bug bounty reports titled "IDOR in order endpoint" or "BOLA in /api/v1/users/{id}".
- Spring Security's `authorizeHttpRequests` with `anyRequest().denyAll()` at the end.
- The OWASP Top 10 and OWASP API Security Top 10, where this is first.
- Data breaches where records were downloaded by counting up an id in the address bar.
