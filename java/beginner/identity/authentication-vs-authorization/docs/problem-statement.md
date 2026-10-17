# Problem Statement

## The scenario

An online store has customers, support staff and an admin. Customers should manage
only their own orders. Support should view any order and issue refunds. Admins
may do everything. The store must tell these people apart, and must give each of
them only what their role allows.

## The naive version

The first version answers every request, and the second answers every request
that carries a valid login. The second looks safe, because anonymous visitors are
turned away. But it treats every signed-in person the same, so a customer can
refund orders and read other people's orders.

## What this project must deliver

- A version with no checks, and a visible result for it.
- A login that checks a password stored only as a slow, salted hash, and returns a session token.
- A gate that turns away requests without a live token with 401.
- A version that checks the login and nothing else, and shows what it allows.
- A policy of roles and ownership that answers 403 when a known person may not do something.
- Sessions that expire, and a demonstration of a stale role against a role looked up each time.
- The same output every run, checked against a golden output file.
