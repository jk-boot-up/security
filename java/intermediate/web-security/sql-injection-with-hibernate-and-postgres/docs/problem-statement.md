# Problem Statement

## The scenario

An online store's product search is written with Hibernate, and the team believes the mapper protects it.

## The naive version

Build the JPQL or native query string from the customer's text; the ORM will keep it safe.

## What this project must deliver

- A real PostgreSQL started in Docker and removed afterwards.
- Concatenated JPQL and native queries that leak data, with the payloads shown.
- Fixed versions with named parameters, the Criteria API and an allow-list for sort keys.
- A database user with read access to one table and the refusal it causes.
