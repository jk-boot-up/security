# Threat Modeling with pytm, Explained

## The idea in one sentence

A threat model tool asks the same questions of every element and flow, and the answers decide what it finds.

## The 5 acts

### 1. The bank, written for pytm

The input is the bank as Java records: 7 elements (a customer, a mobile app, an API gateway, a payments service, an accounts database, an audit log and a card processor) in 4 boundaries (Device, DMZ, Internal, Partner), and 8 flows with their protocol and port. The program writes them as 66 lines of Python: a TM, one Boundary per boundary, one object per element (Actor, Process, Server, Datastore, ExternalEntity), one Dataflow per flow, and tm.process() at the end.
pytm 1.4.0 checks that model against its library of 113 threats (one more is deprecated). Each threat names the kinds of element it applies to, a severity and a condition: AC06, Using Malicious Files, Very High, applies to every Server when isHardened is False or hasAccessControl is False.
The result for the plain project's draft, with a card vault the app writes to directly: pytm runs it without a complaint and reports 9 new findings, 4 on the card vault and 5 on the new flow. The plain project's model check reported 2 problems (a skipped boundary and a store with no owner): pytm reads the answers about each element, not the shape of the picture.

### 2. What pytm finds when nothing is said

The input is the model with no answers: no line sets a control, so every control is False, the worst case.
The result: 170 findings from 73 of the 113 threats; by severity 16 Very High, 59 High, 85 Medium, 8 Low and 2 Very Low. Per element, out of the threats that apply to its kind: the API gateway and the payments service 44 of 54 (Server), the mobile app 34 of 44 (Process), each store 4 of 5 (Datastore), the customer 0 of 0 (no threat applies to an Actor), the card processor 0 of 2 (ExternalEntity), and each of the 8 flows 5 of 15 (Dataflow). Worked out: 44 + 44 + 34 + 4 + 4 + 0 + 0 + 8 x 5 = 170.
The 16 Very High findings come from 11 threats, such as AC06 Using Malicious Files, AC17 Session Hijacking and INP05 Command Line Execution through SQL Injection. The plain project's STRIDE rules gave 20 candidates for the same bank.

### 3. Every answer changes the findings

The input: the same bank with more answers at each stage: encryption (30 lines that set a control), authentication (48), access control and input handling (74), resilience (114).
The result: 170 findings, then 167 (3 gone), 159 (8 gone), 111 (48 gone) and 94 (17 gone); no stage adds a finding.
One finding worked out, AC06 on the API gateway, condition isHardened is False or hasAccessControl is False. Up to authentication both answers are False, so the finding is reported. At access and input hasAccessControl becomes True, but isHardened is False is still True, and True or False = True: still reported. At resilience the line gateway.controls.isHardened = True makes both tests False, and False or False = False: not reported.
The class Condition redoes this for every threat whose condition only asks about answers: 91 of the 113. Over every element and stage that is 730 checks, and all 730 give pytm's result. An untrue answer removes a finding just the same, so each answer needs an owner and a test.

### 4. A diagram and a report

The input: the model at the last stage, and three commands: python3 bank.py --dfd for the graph text, dot -Tsvg to draw it, and python3 bank.py --report with a template.
The diagram: 183 lines of graph text with 4 boundaries (DMZ, Device, Internal, Partner), 7 boxes, one per element, and 8 arrows numbered (1) to (8) in flow order.
The template is ours: a heading with {tm.name}, then {findings:repeat:...} with one line per finding, * {{item.threat_id}} {{item.target}}: {{item.description}}. The report has 102 lines of Markdown, 94 of them findings, the first being INP02 Mobile app: Overflow Buffers.
The findings, the diagram and the report come from one model, so they cannot disagree with each other.

### 5. The bill

The input: a design change, refunds sent from the payments service to the card processor over HTTP on port 80. f9 is in no list of Answers, so no answer is given for it.
The result, with the model regenerated from the design: 94 findings become 99, all 5 new ones on the Refund flow: DE01 Interception, AC05 Content Spoofing, DE03 Sniffing Attacks, CR06 Communication Channel Manipulation (High) and CR08 Client-Server Protocol Manipulation.
The XML findings worked out: 5 findings mention XML, 2 of them INP19 XML External Entities Blowup, on the two servers. Its condition is usesXMLParser is False or disablesDTD is False. The bank never sets either, so both tests are True, and True or True = True: reported. A person reads it and says it does not apply.
pytm holds no owner, no accept or transfer and no review date, and does not check that a mitigation exists: the answers are claims. The register of the plain project stays.

## The verdict

Describe the system once and generate the model from it, give each answer an owner and a test, start from the worst case and earn each finding away, keep decisions in a register, and rerun the tool at every design change.

## How to recognise this in code you did not write

- `from pytm import` in a repository.
- A `--dfd` diagram in a design document.
- A list of CAPEC ids next to a design.
- Findings sorted by severity in a report.

## Where you have already met this

- A `tm.py` file in a repository.
- A data-flow diagram in a design document.
- A STRIDE spreadsheet.
- A list of findings sorted by severity that nobody has read.
