# Problem Statement

## The scenario

An online store lets customers download invoices by name, upload product photos, and return to the page they came from after signing in.

## The naive version

Join the customer's name to the folder, save the upload under the customer's name, and redirect to the address in the link.

## What this project must deliver

- A real temporary file tree with invoices, a similar sibling folder, secrets, settings and a web root.
- Three download implementations: naive, text-prefix and layered.
- Naive and fixed uploads, with magic-byte detection and a download handler.
- A return-link check and an exact host match.
