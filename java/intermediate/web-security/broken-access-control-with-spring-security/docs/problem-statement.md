# Problem Statement

## The scenario

An online store's Spring Boot API serves orders, delivery addresses, refunds, user management and a sales report to customers, support staff and admins.

## The naive version

Write a few URL rules for the paths that seem sensitive, end with permitAll() or authenticated(), look orders up by id, and trust the framework to do the rest.

## What this project must deliver

- A naive Spring Security configuration with three common mistakes, and the attacks that pass it.
- A guarded configuration built from one rule list, with denyAll() last.
- Ownership in the query, and @PostAuthorize compared with it.
- Method security on the refund service, which holds for callers with no URL.
- A denial log from AuthorizationDeniedEvent, with no credentials.
- A route inventory test that reads Spring MVC's handler mapping.
