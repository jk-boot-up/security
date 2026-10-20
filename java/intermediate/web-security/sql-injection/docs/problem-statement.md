# Problem Statement

## The scenario

An online store has a product search and a sort option, backed by a database that also holds customer accounts and orders.

## The naive version

Build the statement by joining text: the search words are only text, so pasting them in must be harmless.

## What this project must deliver

- A small SQL engine in plain Java that reads statements the way a real database does.
- A search built by pasting, and three attacks on it: a hidden product, a union that leaks accounts, and a stacked statement.
- The fixed search: parameters for values and an allow-list for the sort column.
- A least-privilege database user that limits what one mistake can reach.
