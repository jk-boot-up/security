# Least Privilege with Spring Security, Explained

## The idea in one sentence

Least privilege means every account holds only the actions its job needs, and Spring Security can enforce it by denying every path without a rule, guarding methods by authority, keeping separation of duties in one rule that no role can switch off, and letting grants expire by themselves.

## The 5 acts

### 1. Deny by default: every path needs a rule

With no credentials, a request for orders gets 401: who are you? Alice, from support, with a wrong password also gets 401. With the right one she gets orders, and 403 for reports, because she is known but not allowed. Dave, an admin, asks for a path nobody wrote a rule for, and gets 403 too.
The last rule is `anyRequest().denyAll()`, so a forgotten endpoint stays closed.

### 2. Method security: the rule sits next to the code

Bob, from the warehouse, tries to create a refund and gets 403: he has no refunds:create. Alice creates refund R-1. She can read her own, and carol, an approver, can read any. Bob cannot read someone else's request. But the method ran for bob before the answer was withheld,
because `@PostAuthorize` judges the result. `@PreAuthorize` stops a call before it runs, so a method with side effects needs that one.

### 3. Separation of duties: two people for a refund

Alice cannot approve her own refund: she has no refunds:approve. Dave, an admin, creates refund R-2 and tries to approve it himself, and is refused: admin power does not remove the rule. Carol approves R-1. Alice creates R-3 and Dave approves it, because an admin may approve someone
else's request. The rule is a bean that the annotation calls, in one place, and no role can switch it off.

### 4. A role hierarchy, and what a stolen credential can reach

With a role hierarchy, an admin includes support, so the staff queue lets alice and dave in and keeps carol from finance and the exporter out. Then the number that matters. The nightly order export ran as dave because that was quick: a stolen token reaches 15 of 15 actions.
The same job as the exporter service account reaches 2 of 15. Alice, bob and carol reach 3 each. The numbers come from asking Spring Security's own authorization manager about each action.

### 5. Access that expires by itself

Carol, in finance, is granted payments:write for ticks 10 to 12, for a month-end fix. At tick 9 the answer is 403, at ticks 10 and 12 it is 200, and at tick 13 it is 403 again. Nobody had to remember to remove it. Alice, who never had a grant, gets 403 even inside the window.
The rule is a custom AuthorizationManager that asks an injected clock, so the demo and the tests control time.

## The verdict

End every rule chain with deny. Give accounts exact authorities and use roles only to name jobs. Put separation of duties in code that roles cannot bypass. Give service accounts their own, smaller authorities, and measure what a stolen token reaches. Prefer grants that expire.

## How to recognise this in code you did not write

- `@PreAuthorize` and `@EnableMethodSecurity` in Spring applications.
- `anyRequest().authenticated()` at the end of a configuration, which should usually be `denyAll()`.
- Service accounts named `admin` or `root`.
- Temporary access in cloud consoles that expires.

## Where you have already met this

- `@PreAuthorize("hasRole('ADMIN')")` on controller methods in most Spring tutorials.
- `anyRequest().authenticated()` at the end of a security configuration, which is one step weaker than `denyAll`.
- Cloud IAM policies with "allow only these actions" and temporary credentials.
- Approval workflows where the person who asked cannot approve.
