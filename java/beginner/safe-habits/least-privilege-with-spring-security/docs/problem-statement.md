# Problem Statement

## The scenario

An online store has support, warehouse, finance, an admin and a nightly export job. Each needs some of the store's fifteen actions, and none needs all of them.

## The naive version

Give the export job the admin role because that was quick, and put `authenticated()` at the end of the rules so that anything not listed is open to anyone who is logged in.

## What this project must deliver

- A request chain that ends with deny, tested with a path nobody listed.
- Method security on refunds with `@PreAuthorize` and `@PostAuthorize`, and a test of what each really does.
- Separation of duties in one bean that an annotation calls, which no role can switch off.
- A role hierarchy, and the count of what each account reaches, taken from Spring Security's own authorization manager.
- A time-limited grant as a custom AuthorizationManager with an injected clock.
