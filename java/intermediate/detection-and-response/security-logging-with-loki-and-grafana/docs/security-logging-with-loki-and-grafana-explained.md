# Security Logging with Loki and Grafana, Explained

## The idea in one sentence

A log store is useful when it can answer a question about the whole system in one line, and a rule is useful when someone has proved that it fires.

## The 5 acts

### 1. Shipping the events

The morning's 8 events are pushed as JSON lines to a real Loki, each answered with 204, with two labels: app=bank and event_type. The label names Loki now knows are app, event_type and service_name (which it adds itself); there are 4 streams, one for each of the 4 event types. A push with no tenant header gets 401 no org id: every request names whose logs it is about.

### 2. Asking questions in LogQL

A text filter for ANA-1 finds 3 events. Who moved 500 or more: ana from 198.51.100.66, ANA-1->MAL-9, success, request req-0006. Everything from that address, in time order: login failure, login failure, login success, money-movement success: someone with ana's password, not ana. Failures by address, counted by Loki: 198.51.100.66 has 2.

### 3. Labels and content

150 more logins, once with the user as a label and once as part of the line: 150 streams against 1. Asking for one user gives 1 line either way. Every label value makes a stream of its own and an entry in the index, so labels are for the few things you always select by, the application and the kind of event, and the rest stays in the line.

### 4. Alerting

Two rules are loaded for the tenant, evaluated every 2 seconds: PasswordGuessing and PasswordGuessingTypo, both inactive and ok. With ana's two failed logins in the logs, nothing is firing. Four failed logins from 203.0.113.9 are shipped; a moment later PasswordGuessing 203.0.113.9 is firing, with the text 203.0.113.9 failed to log in 3 times or more in 5 minutes. The rule with the typo in the field name, outcom, is still inactive and ok: it runs without a complaint and never fires. A rule is code: test it with a known bad event.

### 5. The bill

Another tenant, shop, asking for the bank's lines gets 0 of 12 and sees no labels. An event stamped ten days ago is refused with 400: the timestamp is too old. Asking Loki to delete the bank's lines through its API gets 404: the route is not there until retention and deletion are configured. Grafana, with a data source that sends the tenant bank, has a healthy connection and gives the same count as Loki; through the shop's data source it gives 0. Whoever can use a data source sees that tenant's logs. Loki does not prove that a line was not changed before it was sent.

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
