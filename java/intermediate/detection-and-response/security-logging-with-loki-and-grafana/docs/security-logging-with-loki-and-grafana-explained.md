# Security Logging with Loki and Grafana, Explained

## The idea in one sentence

A log store is useful when it can answer a question about the whole system in one line, and a rule is useful when someone has proved that it fires.

## The 5 acts

### 1. Shipping the events

The input is the morning's 8 security events, each a JSON line such as the transfer: request_id req-0006, event_type money-movement, actor ana, action transfer, resource ANA-1->MAL-9, source_ip 198.51.100.66, outcome success, amount 500. Each is pushed with the header
X-Scope-OrgID: bank (the tenant) and two labels, app=bank and event_type.
The result: 8 answers of 204. Loki keeps one stream per distinct label set: login 4 lines, access-decision 2, money-movement 1, admin-change 1, so 4 streams, as worked out from the label sets. The label names are app, event_type and service_name, which Loki adds.
A push with no tenant header is answered 401 no org id.

### 2. Asking questions in LogQL

The input is four LogQL queries. Query 1 is a text filter, {app="bank"} |= "ANA-1": its answer is 3 lines, req-0002, req-0006 and req-0008. Query 2 selects the money-movement stream, reads the JSON fields and keeps action="transfer" with amount >= 500: the answer is actor ana,
source_ip 198.51.100.66, resource ANA-1->MAL-9, outcome success, request_id req-0006.
Query 3 asks for everything from 198.51.100.66, in time order: login failure, login failure, login success, money-movement success: Ana's password, not Ana. Query 4 is a metric, sum by (source_ip) (count_over_time(... | json | outcome="failure" [1h])): 198.51.100.66 has 2 failures.

### 3. Labels and content

The input: 150 more logins, sent twice. Once labelled app=bank-labelled, event_type=login and actor=user0 to user149; once as app=bank-content, event_type=login, with the actor only inside the JSON line.
The result, worked out and counted by Loki: labelled, 1 app x 1 type x 150 actors = 150 streams, and Loki reports 150; content, 1 x 1 = 1 stream, and Loki reports 1. Asking for user7 returns 1 line both ways: by the label, from 1 of 150 streams; by the content, the 1 stream read
and filtered. The rule of thumb: a label for the few things you always select by, and everything else, such as users, addresses and amounts, in the line.

### 4. Alerting

The input is two rules, evaluated every 2 seconds: PasswordGuessing, sum by (source_ip) (count_over_time({app="bank"} | json | outcome="failure" [5m])) >= 3, and PasswordGuessingTypo, the same with the field misspelt outcom.
Before: both rules are inactive/ok; 198.51.100.66 has 2 failures, 2 >= 3? no, so nothing fires. Then 4 failed logins as ben from 203.0.113.9 are shipped. After a moment: 203.0.113.9 has 4, 4 >= 3? yes, so PasswordGuessing fires for 203.0.113.9, with the text
"203.0.113.9 failed to log in 3 times or more in 5 minutes". The typo rule counts nothing, because no line has a field outcom, and stays inactive/ok: no error, and it never fires. So a rule must be tested with a known bad event.

### 5. The bill

The input: the tenant shop asks for {app="bank"}. The result: 0 lines, while the tenant bank sees 12, and no labels at all. An event stamped ten days ago meets the limit reject_old_samples_max_age 168h = 168 / 24 = 7 days; 10 > 7, so it is refused with 400, timestamp too old.
Asking Loki to delete the bank's lines is answered 404: the route is not there until deletion is configured, and no route changes a line once stored.
Grafana, with one data source per tenant header: bank, connection OK and 6 failures, the same as Loki's own 6; shop, connection OK and 0 bank lines. Left to the team: who may use a data source (that is who reads the logs), proof that a line was not changed before it was sent, retention, index size and keeping secrets out of the lines.

## The verdict

Ship structured events with few labels, keep the rest in the line, separate tenants, test every rule with a known bad event, control who can use a data source, refuse or bound old events, and let the sender prove the lines were not changed.

## How to recognise this in code you did not write

- `{app="..."} | json | field="value"` in a query.
- `X-Scope-OrgID` in a request.
- A rule file with `alert:` and `expr:`.
- A Grafana data source with a custom header.

## Where you have already met this

- A Grafana dashboard with an "Explore" button.
- A `{app="..."}` selector in a query.
- A log agent such as Promtail or Alloy configured with labels.
- A page at night because of an alert rule.
