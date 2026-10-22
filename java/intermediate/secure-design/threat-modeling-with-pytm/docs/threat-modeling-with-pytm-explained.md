# Threat Modeling with pytm, Explained

## The idea in one sentence

A threat model tool asks the same questions of every element and flow, and the answers decide what it finds.

## The 5 acts

### 1. The bank, written for pytm

The design has 7 elements, 8 flows and 4 boundaries, written by the program as 66 lines of Python for pytm 1.4.0: boundaries, then actors, processes, servers, stores and external entities, then flows with their protocol and port. pytm's library has 113 threats, each with a condition over the answers given about an element or a flow. The draft of the plain project, with a card vault and a flow from the app that skips a boundary, runs without a complaint and gets 9 findings about the new store and flow; the plain project's model check reported 2 problems for the same draft. pytm judges the answers about each element, not the shape of the picture.

### 2. What pytm finds when nothing is said

With no answers, pytm reports 170 findings from 73 different threats: 16 Very High, 59 High, 85 Medium, 8 Low and 2 Very Low. They are spread over every element and flow, 44 each for the API gateway and the payments service. The most severe are using malicious files, catching an exception from a privileged block, and session hijacking, server side and client side. The plain project's STRIDE rules gave 20 candidates for the same bank; pytm checks each of its 113 threats against every element and flow it applies to, and starts from the worst case when nothing is said.

### 3. Every answer changes the findings

With no answers 170 findings. Declaring that traffic and stores are encrypted: 167, 3 gone. That callers and services are authenticated: 159, 8 gone. Access control, least privilege and input handling: 111, 48 gone. Hardening, limits and logging: 94, 17 gone. The findings are a function of the answers: an answer that is not true makes a finding disappear, which is why each answer needs an owner and a test.

### 4. A diagram and a report

--dfd writes a graph for Graphviz; rendered, the picture has 7 nodes, 8 arrows and 4 trust boundaries. --report fills a template of our own: 94 findings listed, each with its element and severity, from 102 lines of Markdown. The diagram and the report come from the same model as the findings, so they cannot disagree with it.

### 5. The bill

A design change, refunds sent to the card processor over plain http: 94 findings become 99, 5 of them about the new flow. The model catches the change only if the model is changed. 5 findings mention XML in a bank that parses none: a library of generic threats gives candidates, and a person says which apply. pytm holds no decision: no owner, no accept or transfer, no review date, no check that a mitigation exists; those stay in the register of the plain project. The Python model is a second copy of the design, so here it is written from the Java design and regenerated with every change.

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
