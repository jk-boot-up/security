# Problem Statement

## The scenario

An online store accepts XML invoices from suppliers and sends e-mails built from templates that include the customer's name.

## The naive version

Parse the file with the library's defaults, and build the e-mail by putting the customer's name into the template text.

## What this project must deliver

- An invoice upload with the JDK's real XML parser, as it comes and as it should be configured.
- An external entity that reads a server file, and an entity bomb.
- A reflection-based expression engine, and the leak when customer text becomes part of the template.
- A logic-less engine with escaped, named values.
