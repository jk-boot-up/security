# Problem Statement

## The scenario

An online bank sees 500 leave a customer's account for an unknown one. The customer says it was not her. The bank must find out who did it, from where and how, from its logs, and prove the logs were not changed.

## The naive version

Log free-text messages for developers, include whatever the code has at hand, card numbers and tokens included, and keep the file on the server. The log then cannot answer the question, holds secrets, accepts forged lines, and disappears with the server.

## What this project must deliver

- A structured security event with the same fields every time and a request id.
- A redactor that removes secrets by field name and masks card numbers and tokens by their shape.
- JSON lines written and read without a library, with every control character escaped.
- A hash-chained log, a chain checker, and a published head hash.
- Shipping to a second store, retention with an anchor, and a volume estimate.
- The same output on every run.
