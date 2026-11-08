# Least Privilege with Spring Security, Explained

## The idea in one sentence

Least privilege means every account holds only the actions its job needs, and Spring Security can enforce it by denying every path without a rule, guarding methods by authority, keeping separation of duties in one rule that no role can switch off, and letting grants expire by themselves.

## The 5 acts

### 1. Deny by default: every path needs a rule

The rule chain in the Shop class is checked from top to bottom, and the first rule that matches the request decides. GET /orders needs orders:read, GET /reports needs reports:read, and so on. The last rule, anyRequest().denyAll(), matches everything else and says no.
Five requests: with no credentials the answer is 401 (who are you?), and with a wrong password still 401. Alice in support may read the orders, 200, but not the reports, 403: she is known, but not allowed. Dave, an admin, asks for /admin/debug, a path nobody wrote a rule for, and gets 403, so a forgotten endpoint stays closed even for an admin.

### 2. Method security: the rule sits next to the code

Two rules sit on the methods of the Refunds class. create() has @PreAuthorize, which checks before the method runs: it needs refunds:create. view() has @PostAuthorize, which runs the method first and then checks the result: the caller must be the requester, or hold refunds:approve.
Bob in the warehouse gets 403 when he creates a refund, and create() never runs. Alice creates R-1. Alice may read her own request, and Carol may read any as an approver. Bob gets 403 on R-1, but the method had already run for him. So a method with side effects needs @PreAuthorize.

### 3. Separation of duties: two people for a refund

The rule on approve() has two halves: hasAuthority('refunds:approve'), and @refundRules.differentPerson(#id, authentication), a small bean that checks that the approver is not the person who asked.
Alice approving her own R-1 gets 403, as she has no refunds:approve. Dave, an admin, creates R-2 and approving it himself gets 403: he holds every permission, but he is the same person. Carol approves R-1, and Dave approves R-3, which Alice created. No role can switch the rule off.

### 4. A role hierarchy, and what a stolen credential can reach

A role hierarchy says that ADMIN includes SUPPORT, FINANCE and WAREHOUSE, so a rule that needs the role SUPPORT also lets an admin in. GET /staff/queue answers 200 to Alice (SUPPORT) and Dave (ADMIN), and 403 to Carol (FINANCE) and the exporter.
The nightly order export once ran as Dave, because that was quick: a stolen token reaches 15 of 15 actions. As the exporter service account, which holds only orders:read and reports:read, it reaches 2 of 15: it can read the orders, 200, and not the secrets, 403. Each count comes from asking Spring Security's own authorization manager about every action.

### 5. Access that expires by itself

Carol in finance gets payments:write for a month-end fix, from tick 10 to tick 12, both included (a tick is one step of the demo's simulated clock). The rule for PUT /payments/** is a custom AuthorizationManager, GrantAuthorization, that allows the request if the caller holds payments:write or has a grant that is active right now.
PUT /payments/P-1 answers 403 at tick 9, 200 at ticks 10 and 12, and 403 at tick 13. Alice, who never had a grant, gets 403 at tick 11. The clock is injected, so the demo and the tests control time.

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
