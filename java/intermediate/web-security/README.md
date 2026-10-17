# Web security

Injection and access control, the two families at the top of the OWASP Top 10 (2025): A01 Broken Access Control and A05 Injection.
Each project attacks an online store or a bank the way real applications are attacked, then fixes it the way production code fixes it.

## The projects

| # | Project | What it shows | Maps to |
| --- | --- | --- | --- |
| 1 | [broken-access-control](broken-access-control) | Order numbers anyone can walk through, a forgotten admin route, a role checked only in the page; one deny-by-default policy and a route inventory | A01, API1, API5, CWE-639, CWE-862 |
| 2 | [broken-access-control-with-spring-security](broken-access-control-with-spring-security) | The same store in Spring Security 7: deny-by-default rules, ownership in the repository query, method security, a denial log and a route inventory read from Spring MVC | A01, API1, API5 |
| 3 | [cross-site-scripting](cross-site-scripting) | A review that becomes part of the page; contextual output encoding and a nonce-based Content-Security-Policy | A05, CWE-79 |
| 4 | [cross-site-scripting-with-spring-boot](cross-site-scripting-with-spring-boot) | Thymeleaf escaping, the th:utext trap, safe attribute, URL and script contexts, and a per-request CSP nonce | A05, CWE-79 |
| 5 | [cross-site-request-forgery](cross-site-request-forgery) | A transfer sent from another site with the customer's cookie; form tokens, SameSite and Fetch Metadata | A01, CWE-352 |

A project with `-with-` in its name is the same lesson with a framework or a local tool, and is its own self-contained project.

## Still to come

server-side-request-forgery, xml-and-template-injection, insecure-deserialization, path-traversal-and-file-upload, and the
`-with-` versions listed in the [intermediate plan](../../../docs/intermediate-plan.md).

## How the module is judged

- **Break it, then fix it.** The weak version fails first; the fix is shown against the same requests.
- **Every attack targets only the project's own code,** on loopback, with harmless markers instead of real payloads.
- **Every claim is a number** that a test checks against a golden output file.
- **A threat model for every project,** with a diagram, in `docs/threat-model.md`.
