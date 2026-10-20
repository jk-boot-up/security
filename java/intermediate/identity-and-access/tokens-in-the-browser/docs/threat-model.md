# Threat Model — Tokens in the Browser: BFF and DPoP

A threat model answers four questions: what are we protecting, who or what could harm it, how, and what do we do about it. This one covers this project's own demo system, not a production deployment. Domain: a single-page banking app.

## Scope

A first-party single-page app and the API it calls. It covers where the access token is kept and what a script on the page, or a copy of the token, can do.
It does not cover how the script got onto the page, the authorization server's own security, or stolen devices.

## What we protect (assets)

- The customer's access token and session.
- The customer's account at the API.

## Who and what acts on the system

- Ana, a customer using the app.
- An attacker with a script running on the page.
- An attacker with a copy of a token, using it from elsewhere.

## Trust boundaries

A trust boundary is a line that data or a request crosses between parts that trust each other differently. Threats cluster where those lines are.

- The page to the rest of the browser: every script on the page has the page's privileges.
- The browser to the BFF and the API: requests carry a cookie or a token.

![Threat model diagram](images/threat-model-diagram.png)

Each dashed box is a trust boundary. A rose tag such as T1 shows where the threat of that number, in the table below, applies.

## Threats, using STRIDE

STRIDE is a checklist of six kinds of threat: **Spoofing** (S), **Tampering** (T), **Repudiation** (R), **Information disclosure** (I), **Denial of service** (D), **Elevation of privilege** (E).

| Id | Kind | Threat | Where it shows up | Control in this project | What is left |
| --- | --- | --- | --- | --- | --- |
| T1 | Information disclosure | A script reads the access token from web storage and the attacker uses it elsewhere | `BrowserPage`, local storage | Keep the token on a BFF, or bind it with DPoP | While the page is open, the script can still send requests through the page |
| T2 | Spoofing | Another site makes the browser send the session cookie to the BFF | `Bff.handle` | `__Host-` SameSite cookie, origin check and a custom header on state-changing requests | A BFF page with cross-site scripting |
| T3 | Elevation of privilege | An injected script uses the session while the page is open | `Bff`, the page | Fix the injection; a strict Content-Security-Policy; short sessions; step-up for sensitive actions | Remains possible by design while the script runs |
| T4 | Spoofing | A copied token, a replayed proof or a saved proof is presented to the API | `ResourceApi`, `DpopProof.ProofCheck` | Binding to a key, and checks of method, address, token hash, time and unused id | A key that scripts can export defeats the binding |

## Assumptions

- The BFF's session store and the authorization server are not compromised.
- In the DPoP design, the private key cannot be exported by scripts (the demo does not model WebCrypto).

## Out of scope

- Preventing the injection: cross-site-scripting and Content-Security-Policy.
- Refresh tokens and sign-in: oauth2-authorization-code-pkce and openid-connect.

## How the tests check this

- `TokensTest` checks the page model, the BFF's cookie, origin and header rules, sign-out, and every DPoP check: signature, key binding, method, address, token hash, time window, replay, key shape and the JSON reader.
- `DemoRunsTest` compares the whole demo with the golden output and checks that it is repeatable.

## Where this model goes next

The next project, saml-basics, looks at the older single sign-on standard and the signature attacks on its XML.
