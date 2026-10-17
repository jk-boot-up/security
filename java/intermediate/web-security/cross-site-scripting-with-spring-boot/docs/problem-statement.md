# Problem Statement

## The scenario

An online store shows reviews, fit notes and search links written by customers on its product pages, rendered with Thymeleaf.

## The naive version

Build the page with th:utext, or with HTML assembled in code, "because the review may contain formatting".

## What this project must deliver

- Pages that show a review escaped with th:text and, once, the trap with th:utext.
- Values written into an attribute, a link and a script, each escaped for its place.
- A strict Content-Security-Policy with a fresh nonce for every page, from Spring Security and a small filter.
- HttpOnly and SameSite session cookies.
- A guard test that lists every template using th:utext.
