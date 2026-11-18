# Attack Trees and Abuse Cases in Threat Dragon, Explained

## The idea in one sentence

A threat model file is a diagram with threats pinned to it; the file is only worth what is true of the system.

## The 6 acts

### 1. The bank as a Threat Dragon diagram

The input is the 8 abuse cases of the plain project, such as AC5: Mallory takes Ana's money by changing the account number. The diagram the program writes has 13 cells: the trust boundaries Internet and Bank network; the actors Ana (customer) and Phone carrier, in Internet; the processes Web app, Transfer API and Approval console and the store Accounts database, in Bank network; and 5 flows: Sign-in, Transfer request, Balance update, Approval request and Text-message code.
The result is one threat per case on the cell it hits: AC1 and AC2 Spoofing on Sign-in, AC3 Information disclosure on Text-message code, AC4 Tampering and AC8 Elevation of privilege on Web app, AC5 Elevation of privilege on Transfer API, AC6 Tampering on Transfer request, AC7 Elevation of privilege on Approval console. All 8 are Open, and the file's threatTop is 8.

### 2. From the tree's numbers to a severity

The input is the tree's steps for AC1, joined by an AND, so both are needed: A1a, build a phishing page that relays the code, 300 EUR and 90%; A1b, she enters the password and the code, 50 EUR and 20%. Costs add up: 300 + 50 = 350 EUR. Chances multiply: 90% x 20% = 18%. The severity rule is High if the cost is at most 500 EUR or the chance at least 50%, Medium up to 5,000 EUR, and Low beyond; AC1 is High, because 350 EUR <= 500 EUR.
The result for all eight: AC1 350 EUR 18% High, AC2 100 EUR 12% High, AC3 1,600 EUR 5% Medium, AC4 2,000 EUR 5% Medium, AC5 50 EUR 90% High, AC6 100 EUR 48% High, AC7 20,000 EUR 20% Low, AC8 5,000 EUR 15% Medium. In the file AC1 reads "type": "Spoofing", "severity": "High", "status": "Open", "score": "cost 350 EUR, chance 18%".

### 3. Checked by Threat Dragon's own validator

The input is five files: bank.json, the bank's model as written; no-title.json, with the summary's title removed; short-type.json, with the diagram type set to "x"; no-id.json, with the id of cell 2 removed; and tiny.json, with the width of cell 2 set to 5. The validator is the schema and the Ajv inside the image owasp/threat-dragon:v2.6.2, used as they are.
The result: bank.json is valid. no-title.json is refused at .summary: should have required property 'title'. short-type.json at .detail.diagrams[0].diagramType: should NOT be shorter than 3 characters. no-id.json at .detail.diagrams[0].cells[2]: should have required property 'id'. tiny.json at .detail.diagrams[0].cells[2].size.width: should be >= 10.

### 4. The plan, step by step, in the file

The input is the plan of the plain project: 1 OWN, check that the account belongs to the signed-in user; 2 CSRF, form tokens and SameSite cookies; 3 PASSKEY, passkeys for sign-in and payments; 4 CSP, strict content security policy and HttpOnly cookies.
The result, step by step: open threats 8, 7, 6, 3, 2 and abuse cases failing 8, 7, 6, 3, 2, and the validator says valid at every step. Threat by threat: AC1, AC2 and AC3 go from Open to Mitigated, In place: PASSKEY; AC4 by CSP; AC5 by OWN; AC6 by CSRF. AC7 and AC8 stay Open: DUAL and PATCH would close them, and they are not in the plan.

### 5. Checks the schema cannot make

The rules: complete-threat, known-status, known-severity, stride-type, mitigated-names-its-defence, unique-numbers, open-flag-matches-threats, flow-ends-exist, threat-top-is-highest-number and every-abuse-case-has-a-threat. The plan's files have 0 problems before and 0 after.
The input: the last file, broken four ways: a Mitigated threat's mitigation emptied, an open cell's hasOpenThreats set false, a flow's target set to a missing cell, and threatTop set to 3. The result: the schema says valid to all four, and the rules find each one: mitigated-names-its-defence at Web app / AC4, open-flag-matches-threats at Web app, flow-ends-exist at Sign-in target, threat-top-is-highest-number with threatTop 3 but highest 8.

### 6. The editor, and the bill

The requests go to the image's own server, started with generated keys on a loopback port: GET /healthz, GET /, GET /api/config and GET /api/threatmodel/organisation/models.
The responses: /healthz 200; / 200, titled OWASP Threat Dragon; /api/config 200, with the sign-in providers bitbucket, github, gitlab and google all false and local true; and the model API 401, not signed in. The bill: the tree has 6 AND and 3 OR gates and the file has none; cost and chance are only text, such as "cost 50 EUR, chance 90%"; and there are no tests in the file: Mitigated is a claim, and the abuse cases prove it.

## The verdict

Generate the model from the thing you can test, validate it with the editor's schema, add rules for what the schema cannot say, keep it in version control, and let the abuse cases in the code decide whether a threat is Mitigated.

## How to recognise this in code you did not write

- A `threat-model.json` file with `diagrams` and `cells`.
- A threat with `status`, `severity` and `mitigation`.
- `tm.Process` and `tm.Flow` in a JSON file.
- A "Local Session" button on a login page.

## Where you have already met this

- A threat model JSON file in a repository.
- A diagram with red numbers on the flows.
- A status column with Open, Mitigated and N/A.
- A "Report" button in a threat modeling tool.
