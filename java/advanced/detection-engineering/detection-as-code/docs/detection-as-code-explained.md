# Detection as Code, Explained

## The idea in one sentence

A detection is code: give it tests and a truth, measure what it catches and what it costs, and keep the numbers.

## The 6 acts

### 1. One rule against three events

The input is the rule (tags T1505.003 and T1059.004; selection_parent: ParentImage ends with /nginx, /httpd, /php-fpm or /java; selection_child: Image ends with /sh, /bash or /dash; filter_healthcheck: CommandLine starts with /bin/sh -c /opt/bank/bin/healthcheck; condition: selection_parent and selection_child and not filter_healthcheck) and three process starts on a bank host.
The result, worked out selection by selection: event 1 (nginx starts sh) is yes, yes, no, so it alerts: caught. Event 2 (the Java server runs the health check) is yes, yes, yes: the filter stops the alert, rightly. Event 3 (apache2 starts sh) is no for the parent: no alert, and the attack is missed.

### 2. A week of logs, each rule scored

The input is a week of a bank's events made by a seeded generator: 21,032 benign, 208 look-alikes that a filter in the rule removes, 94 look-alikes no filter removes, and 92 attacks in 366 events: 65 a rule is written for, 17 evasions of those rules and 10 that no rule is written for. Every event carries its truth.
The result is each rule against the truth: attacks caught out of those it is written for, false alarms, precision (P) and recall (R). All rules: 65 of 82 caught, 57 false alarms. For log-clearing: R = 9 / 11 = 0.82; P = 9 / (9 + 16) = 0.36.

### 3. False-alarm budgets and tuning

The input is a budget per rule in rules/budgets.yaml: the false alarms a day an analyst will still read. The result: mean, worst day and days over budget for each rule; log-clearing, scheduled-task-persistence and web-server-shell are OVER.
How a budget is checked, for log-clearing (budget 2): false alarms on days 1 to 7 are 1 3 2 3 1 3 3, the mean is 16 / 7 = 2.3, and days 2, 4, 6 and 7 are over: 4 of 7.
Before the health-check filter is dropped, web-server-shell has 6 false alarms; after, 77, with 13 on the worst day. Moving failed-login-burst's threshold N (failed logins from one address in 5 minutes, now 10): 5 changes nothing, 15 removes the 3 false alarms and still catches 8 of 10, 20 catches only 6.

### 4. Tests and mutants

The input is the test file beside each rule, rules/tests/NAME.json: a few events and whether the rule must alert (match) or must not (no match). For web-server-shell: nginx, httpd, php-fpm and the application server starting a shell must match, a shell that only mentions the health check later must match, and a worker, a person's bash, the health check and python must not.
The result: 9 files, 83 cases (47 should match, 36 should not), 0 failing. Then the mutants: the mutator drops a value from a list (tighten), drops a filter or turns the first and into or (loosen), drops a field, or moves a threshold or window. For web-server-shell each of its 9 mutants makes 1 to 4 cases fail. Over every rule: 80 mutants, 30 loosen and 50 tighten, all 80 killed.

### 5. ATT&CK coverage

The input is two lists: the techniques each rule is tagged with, and the technique of every labelled attack. The result is a matrix, one row a technique: the attacks that used it, how many some rule caught, a status and the rule.
How the status is decided: GAP when attacks used it and no rule is tagged with it (T1003.002, T1021.004, T1046, T1048.003, T1053.006); partial when a rule is tagged and misses some attacks; covered when it catches every one; untested when a rule is tagged and no labelled attack used it (T1059.004). Covered: 4 of 20.

### 6. pySigma and OpenSearch

The input is the rules folder and the pySigma OpenSearch backend: web-server-shell becomes (ParentImage:(*\/nginx OR *\/httpd OR *\/php\-fpm OR *\/java)) AND (Image:(*\/sh OR *\/bash OR *\/dash)) AND (NOT CommandLine:\/bin\/sh\ \-c\ \/opt\/bank\/bin\/healthcheck*). The backend does not support correlation rules, so failed-login-burst is not converted.
The result: each query run in a one-node OpenSearch over the same events returns exactly the event ids the Java evaluation alerted on, 8 of 8, with 0 events decided differently. For the correlation, Java counts over the failed logins OpenSearch returned: 11 alerts, the same as Java alone.

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
