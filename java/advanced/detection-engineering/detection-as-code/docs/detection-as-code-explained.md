# Detection as Code, Explained

## The idea in one sentence

A detection is code: give it tests and a truth, measure what it catches and what it costs, and keep the numbers.

## The 6 acts

### 1. The recorded logs

A seeded generator makes logins, process starts and web requests of a bank, look-alikes such as a health check run by the application server, and 92 attacks of which 65 are meant for a rule, 17 are evasions of those rules and 10 have no rule.

### 2. Rules scored

Each rule is scored against the truth: attacks caught and missed, false alarms, precision and recall. The misses are the evasions: a renamed tool, another way to spell a command.

### 3. False-positive budgets

The budget is set with the SOC. log-clearing, scheduled-task-persistence and web-server-shell go over on some days. Removing the health-check filter takes web-server-shell from 6 to 77 false alarms.

### 4. Tests and mutants

Each rule has a file of cases that should match and should not. The mutator drops a filter, a field or a value, changes an and to an or, moves a threshold: all 80 changed rules fail a test.

### 5. ATT&CK coverage

The matrix has 20 techniques: 4 covered, 10 partial because of evasions, 5 with attacks and no rule, and 1 tagged and never attacked in the logs.

### 6. pySigma and OpenSearch

pySigma 1.5.1 with the OpenSearch backend converts 8 rules to Lucene queries; the backend does not support the correlation rule, so the count is done over the failed logins OpenSearch returns. 8 of 8 queries return exactly the events of the Java evaluation over 21,700 events.

## The verdict

Keep rules in a repository with tests and tags, score them on labelled logs, set a false-alarm budget per rule, mutate the rules to test the tests, and read coverage from attacks, not from tags.

## How to recognise this in code you did not write

- A rule that has not fired in a year and has no test.
- A rule that fires 200 times a day.
- A coverage chart made of tags.
- A filter nobody can explain.

## Where you have already met this

- Rules edited in a SIEM console with no history.
- An alert that fires 200 times a day and is muted.
- A coverage slide that lists techniques with no test.
- A rule that nobody knows still works.
