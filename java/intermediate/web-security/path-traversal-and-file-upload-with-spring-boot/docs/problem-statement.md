# Problem Statement

## The scenario

An online store serves invoices from a folder and accepts product photos from staff, over Spring Boot.

## The naive version

Resolve the name from the request against the folder, keep the uploaded file under its own name, and redirect to the address in the request.

## What this project must deliver

- Three versions of a real Spring Boot shop over a temporary folder layout.
- Downloads, uploads and redirects over real HTTP, with a real symbolic link.
- Spring's static handler and multipart limits, shown for what they do and do not do.
- A fixed shop with layered checks.
