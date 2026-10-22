# Problem Statement

## The scenario

An online store keeps the cart in a JSON cookie and reads it back with Jackson, with a note whose class the cookie names.

## The naive version

Let the data say which class to build, so that new kinds work without code changes.

## What this project must deliver

- Real Jackson 3 mappers for each configuration.
- A harmless gadget class and a marker file that shows whether it ran.
- Type ids by name, a polymorphic type validator, and a data-only request.
- Stream limits for depth, text and numbers.
