# Broken Access Control with Spring Security, Explained

## The idea in one sentence

Spring Security enforces exactly the rules it is given, so broken access control in a Spring application is usually a missing or too-broad rule; the fix is deny by default in the filter chain, ownership in the query, method security for rules that must hold everywhere, and a test that every mapped route has a rule.

## The 5 acts

### 1. A Spring store with three common mistakes

The naive store's filter chain has three rules, in order: /account and /orders/** need authenticated(), /admin/users needs hasRole("ADMIN"), and anyRequest() is permitAll().
Ana's GET /orders/1001 answers 200, and so does GET /orders/1002, ben's order: the rule said signed in, not whose order. Signed out, GET /admin/reports answers 200, because permitAll() let through a route nobody listed. GET /admin/users answers 403, but DELETE /admin/users/ben answers 200: the pattern /admin/users does not cover /admin/users/ben.
The result: ben's account is gone, 7 of the plain project's 9 attacks succeed, and only DELETE /orders/{id}, written carefully, refuses ana with 403. The page still hides the admin links.

### 2. Deny by default in the filter chain

The fixed filter chain gives every route its own rule, by method and path, and anyRequest().denyAll() is the last rule.
Signed out, GET /admin/reports now answers 401: who are you? Ana gets 403 for the report, the refund, listing users and DELETE /admin/users/ben. PUT /orders/1001, a method nobody wrote a rule for, answers 403, and so does /admin/debug, a path nobody wrote, even for dave the admin: no rule, no access.
But the rule for GET /orders/{id} can only say signed in. A URL rule cannot know whose order 1002 is.

### 3. The object-level check, in the query

After the fix, the four attacks on ben's orders answer 404. The lookup is findByIdAndOwner(id, ana), so ben's order is not found, exactly like a missing order. Sam from support reads order 1002 with 200, because staff may read any order.
The same check written with @PostAuthorize runs after the method: ana opens ben's gift link and gets 403, and the lookup had already run. A 403 confirms that the order exists, and the work was already done. Ben opens his own gift link with 200.
In total, 0 of 9 attacks succeed, and 6 of 6 honest requests still work.

### 4. Method security, and a log of every refusal

RefundService.refund carries @PreAuthorize("hasAnyRole('SUPPORT', 'ADMIN')"), so the rule travels with the method. A nightly job calls the refund service directly, with no URL and no filter chain in between. Running as ana, a CUSTOMER, it is refused; running as sam, from SUPPORT, it is recorded.
22 refusals have been logged so far, for example DENY someone signed out GET /admin/reports, DENY ana DELETE /admin/users/ben, DENY ana GET /orders/{id}: order 1002 is not theirs, and DENY ana RefundService.refund. No password or Authorization header appears in the log.
Spring publishes an AuthorizationDeniedEvent for each refusal of the filter chain and of method security; the store logs the object-level refusals itself.

### 5. The route inventory, and the bill

The route inventory reads every route the controllers serve from Spring MVC's handler mapping: 11. A developer adds GET /admin/export and forgets its rule; the inventory lists [GET /admin/export] and the build fails. Until it has a rule, even dave the admin is refused with 403: denied by default.
The bill: rules now live in two places, the route list and the method annotations, and the inventory test keeps the route list complete. Every object check needs the owner in the query, and @PostAuthorize is a fallback for when it cannot be there.

## The verdict

End every SecurityFilterChain with anyRequest().denyAll(). Write one rule per method and path, from one list. Look objects up with the owner in the query. Put rules that must hold for every caller on the service method with @PreAuthorize,
and use @PostAuthorize only when the check cannot be in the query. Publish refusals as events and log them without secrets. Read the handler mapping in a test, and fail the build when a route has no rule.

## How to recognise this in code you did not write

- `anyRequest().permitAll()` at the end of a configuration: the first thing to look for in a review.
- `requestMatchers("/admin/users")` where `/admin/users/**` was meant.
- `findById(id)` in a controller that serves customers.
- `@EnableMethodSecurity` and `@PreAuthorize` on services.

## Where you have already met this

- `authorizeHttpRequests` with `anyRequest().denyAll()` or `anyRequest().authenticated()` at the end of a `SecurityFilterChain`.
- `@PreAuthorize` and `@PostAuthorize` on service methods, switched on by `@EnableMethodSecurity`.
- Spring Data methods such as `findByIdAndOwner(id, owner)`.
- Pen-test findings titled IDOR, BOLA or BFLA in a Spring application.
