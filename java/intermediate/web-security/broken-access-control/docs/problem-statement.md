# Problem Statement

## The scenario

An online store's API serves orders, delivery addresses, refunds, user management and a sales report to customers, support staff and admins. Its checks were written inside each handler,
over time, by different people.

## The naive version

Check that the caller is signed in, hide the admin links in the page, and trust that nobody will change a number in the address bar or send a request the page does not offer.

## What this project must deliver

- A naive store with each typical mistake: IDOR, a forgotten route, a check only in the page, an unchecked DELETE, and another customer's data changed.
- A guarded store with the same handlers and one policy in front of them: deny by default, roles, and object-level ownership.
- A log of every refusal, with who and why and never a token.
- A route inventory test that fails when a route has no rule.
- The same rules served over real HTTP.
