# Problem Statement

## The scenario

The online store from the plain project, now built with Spring Security. It has customers, support staff and an
admin, and must tell them apart and give each only what their role allows.

## The naive version

Installing Spring Security and permitting every request, or requiring a login and nothing more. Both look
secured and neither decides what a signed-in person may do.

## What this project must deliver

- A Spring Security configuration that permits everything, with a visible result.
- HTTP Basic authentication against BCrypt-hashed passwords, with a 401 for no or wrong credentials.
- A configuration that requires a login only, and shows what it allows.
- `@PreAuthorize` rules for view, cancel and refund, with a 403 when a known person may not.
- A forgotten rule, and a second layer of rules on the address that catches it.
- A count of password checks that shows HTTP Basic pays on every request.
- The same output every run, checked against a golden output file.
