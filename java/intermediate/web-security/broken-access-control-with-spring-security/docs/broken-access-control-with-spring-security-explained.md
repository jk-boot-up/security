# Broken Access Control with Spring Security, Explained

## The idea in one sentence

Spring Security enforces exactly the rules it is given, so broken access control in a Spring application is usually a missing or too-broad rule; the fix is deny by default in the filter chain, ownership in the query, method security for rules that must hold everywhere, and a test that every mapped route has a rule.

## The 5 acts

### 1. A Spring store with three common mistakes

The first store's rules are: /account and /orders/** need a sign-in, /admin/users needs the admin role, and anyRequest().permitAll() covers the rest. Ana reads her own order 1001 and gets 200, and ben's order 1002 and also gets 200:
the rule said signed in, not whose order. Someone signed out reads /admin/reports and gets 200: permitAll let through a route nobody listed. Ana gets 403 for GET /admin/users, but 200 for DELETE /admin/users/ben, because the
pattern /admin/users does not cover /admin/users/ben, and ben's account is gone. Of the plain project's nine attacks, seven succeed; the one handler written carefully refuses ana with 403. The page hid the admin links from ana, which protected nothing.

### 2. Deny by default in the filter chain

The guarded store builds its filter chain from one list of rules, one per method and path, and anyRequest().denyAll() is the last rule. Someone signed out reading /admin/reports gets 401; ana gets 403 for the report, the refund,
the user list and the account deletion. A PUT that nobody wrote a rule for gets 403, and so does dave the admin asking for /admin/debug, a path nobody wrote. But the rule for GET /orders/{id} can only say signed in: a URL rule cannot know whose order 1002 is.

### 3. The object-level check, in the query

Ana's four attempts on ben's orders, reading order 1002, reading his gift order from a leaked link, sending it to her address and cancelling it, all answer 404: the lookup is findByIdAndOwner(id, ana), so ben's order is not found, like a missing order.
Sam from support reads order 1002 with 200, because staff may read any order. The same check written with @PostAuthorize instead answers ana's gift link with 403, and the lookup ran: the 403 confirms the order exists, and the work was already done. Ben opens his own link with 200.
Against the guarded store, 0 of the 9 attacks succeed, and all 6 honest requests still work.

### 4. Method security, and a log of every refusal

RefundService.refund carries @PreAuthorize("hasAnyRole('SUPPORT', 'ADMIN')"), so the rule travels with the method. A nightly job running as ana that calls the refund service directly, with no URL in between, is refused; the same job running as sam is recorded.
By now 22 refusals are logged, such as "DENY someone signed out GET /admin/reports", "DENY ana DELETE /admin/users/ben", "DENY ana GET /orders/{id}: order 1002 is not theirs" and "DENY ana RefundService.refund". No password or Authorization header appears in the log.
The filter chain and method security publish an AuthorizationDeniedEvent for their refusals; the store logs its object-level refusals itself.

### 5. The route inventory, and the bill

The route inventory reads every route the controllers serve from Spring MVC's own handler mapping: 11. A developer has added GET /admin/export and forgot its rule, so the routes without a rule are [GET /admin/export], and the route inventory test fails the build.
Until it has one, even dave the admin is refused with 403, denied by default. The bill: rules now live in two places, the route list and the method annotations, and the inventory test keeps the first complete; and every object-level check needs the owner in the query,
with @PostAuthorize as a fallback for when it cannot be there.

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
