# Problem Statement

## The scenario

An online store shows reviews written by customers. Anyone can write one, and every visitor's browser reads the product page, including visitors who are signed in.

## The naive version

Paste the review into the page, or delete the characters that look dangerous on the way in and paste the rest.

## What this project must deliver

- A demonstration, with a harmless bold tag, that unencoded text becomes part of the page.
- An encoder for each output context: between tags, in a quoted attribute, in a URL parameter and in a script string, each tested.
- A comparison with an input filter, which changes honest text and misses the attribute context.
- A real page with a fresh nonce, a strict policy and an HttpOnly session cookie, and an evaluator that decides which scripts may run.
- The same facts printed on every run, though the nonces are random.
