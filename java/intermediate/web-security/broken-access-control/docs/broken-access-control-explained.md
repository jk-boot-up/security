# Broken Access Control, Explained

## The idea in one sentence

Broken access control is a server that does what any signed-in caller asks, because the check for whose object it is or which role may act was missing, misplaced or only in the page; the fix is one policy that runs before every handler, refuses whatever it has no rule for, and checks ownership of every object.

## The 5 acts

### 1. Walking order numbers

Ana reads her own order 1001. Then she tries the numbers 1000 to 1010: four orders answer, two of them other customers': 1002, ben's, and 1004, cara's. Every order is
one number away. Ben's gift order has an id nobody could guess, but ben shared the link, and ana opens it: a hard-to-guess id is not a permission. Signing in proved who ana is;
nothing asked whether the order is hers. This is IDOR, CWE-639, and API1 in the API list.

### 2. Routes that nobody checks

The page ana sees shows only orders and account: the admin links are hidden. Ana sends the refund request by hand, and it succeeds: the role was checked only in the page.
Someone signed out asks for /admin/reports, and gets it: nobody links to it, but it is still a route. Ana's GET /admin/users is refused with 403, but her DELETE /admin/users/ben succeeds,
and ben's account is gone: one method was checked and the other forgotten. These are missing function-level checks, CWE-862, and API5 in the API list.

### 3. Sideways and upwards

Sideways, with the same role and someone else's data: ana sends ben's order 1002 to her own address, 12 Palm Street. Upwards, a higher role's function: her refund of 1002 is recorded.
Against the naive store nine attacks are tried, and seven succeed, through five of its nine routes. The one route written carefully, DELETE /orders/{id}, refuses ana with 403: the checks
were not all wrong, they were each written separately, and some were forgotten.

### 4. One choke point, deny by default

The same handlers, with every check taken out of them and put in one policy that runs before any handler. Each of the nine attacks is refused: someone else's order answers 404,
signed out answers 401, the wrong role answers 403. Attacks that succeed: none of nine. Honest requests that still work: six of six, including sam from support reading any order and
ben cancelling his own. Someone else's order answers 404, like a missing one, so the reply does not confirm that the order exists. Nine refusals are logged with who asked and why,
and no token appears in the log.

### 5. Keeping it right, and the bill

Today no route lacks a rule. A developer adds GET /admin/export and forgets its rule: the route inventory test lists it and fails the build, and until it has a rule even dave the admin is
refused, denied by default. Over real HTTP on this machine the same rules give 200 for ana's order, 404 for ben's, 403 for the DELETE and 401 for the report signed out. The bill: the naive store
kept its checks in nine handlers, nine places to forget one; the guarded store keeps them in one table that a test can read. Every object-level check needs a lookup of who owns the object, and every
refusal is a log line to keep and watch.

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
