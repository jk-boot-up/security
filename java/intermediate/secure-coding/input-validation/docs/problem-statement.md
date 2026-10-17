# Problem Statement

## The scenario

An online store accepts orders and new accounts from forms on its site, and from anything else that can send a request to the same address.

## The naive version

Check the values in the page, accept whatever arrives, and use strings everywhere, comparing names as typed.

## What this project must deliver

- A checkout that validates every field on the server, refuses unknown fields and prices from the catalogue.
- One small type per kind of value, with parameterized tests of accepted and refused values.
- Login names in one canonical form, display names in NFC with a look-alike check.
- Size limits for bodies, carts and fields, and a measured comparison of a nested pattern with a linear check.
- Error messages that name the field and the rule, and never repeat the raw input.
