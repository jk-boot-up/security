# Spec — XML External Entities and Template Injection

## Purpose

Teach XXE and template injection by reading a server file through an invoice upload and a secret through an e-mail template, then fixing each by refusing what the reader should never run.

## Not in scope

- XML signatures and SOAP: the saml-basics project covers signature wrapping.
- Sandboxing a template engine that customers must be able to program.
- Other parser libraries: the settings are shown for the JDK's DOM parser.

## The problem

Developers parse files and render templates with default settings, which run instructions found in the input.

## The pieces

| Role | Class |
| --- | --- |
| The invoice and its parsers | `Invoice`, `InvoiceParser`, `NaiveInvoiceParser`, `SafeInvoiceParser`, `InvoiceException` |
| The data a template can reach | `Customer`, `App`, `AppConfig` |
| The template engines | `TemplateEngine`, `ExpressionEngine`, `LogicLessEngine` |
| Entry point | `XmlTemplateDemo` |

## Requirements

1. The default parser puts a server file into the invoice through an external entity.
2. The safe parser refuses any DOCTYPE, oversized uploads and other documents, and still reads honest invoices.
3. The expression engine reveals an application secret when customer text is part of the template.
4. The logic-less engine leaves such text as text, escapes markup and does not scan its own output.
5. The demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 10, all passing |
| Demo output | 23 lines, identical on every run |
| Acts | 5 |
| Video | 06:04, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
