# Detecting Attacks with OpenSearch, Explained

## The idea in one sentence

A detection is a question asked of the log on a schedule; a query can count and group, but it cannot remember the previous event.

## The 5 acts

### 1. One Monday of logs, in OpenSearch

The input is the plain project's Monday: 772 events, sent with the bulk API into an index whose mapping was set before the first event. Read back, the mapping is: boolean success; date time; keyword actor, attack, city, ip, role, ticket, type and user.
One event read back as a document, u042's first failed login: time 2026-03-02T12:00:00Z, type login, user u042, ip 198.51.100.7, city Singapore, success false, and attack B, a label kept only for scoring.
The result, counted by OpenSearch with _count: 768 logins, 4 role grants, and 113 events with an attack label, from 4 attacks. No rule reads that field.

### 2. A first version of each rule, as a query

The input is the brute-force query with a threshold of 3, read back as its four parts: filter type = login and success = false; group by user (a terms aggregation); bucket 5m (a date_histogram with fixed edges); threshold min_doc_count 3.
The buckets it returns: u042 at 12:00 with 30 failures, attack B; u010 and u011 at 09:10 with 6 each, false. The four first queries together: brute-force(3) 3 alerts, 1 true; spraying(8) 1, true; privilege-change 2, 1 true; success-after-failures(1) 23, 2 true. In all 29 alerts, 5 true and 24 false; attacks A, B and D are noticed.
Why success-after-failures(1) is noisy: its parts are filter type = login, group by ip, bucket 1h, threshold params.f >= 1 && params.s >= 1. A query cannot say "then", so a typo and a success from one address in the same hour is enough.

### 3. Tuning one threshold

The input is the brute-force query with min_doc_count 1, which returns every account's 5-minute buckets of failures: 102 buckets. The largest normal bucket has 6 failures (2 buckets, such as u010 at 09:10); the largest attack bucket has 30 (u042 at 12:00).
The safe range is worked out: more than 6 and at most 30, so min_doc_count 7 to 30; 10 is chosen.
The sweep changes that one number in the query: 3 and 5 give 3 alerts, 2 false; 10 and 25 give 1 alert, true; 40 gives no alert, and attack B is MISSED.

### 4. The tuned set as monitors of the alerting plugin

The input is the tuned set saved as monitors in the cluster. brute-force(10), for example, is a bucket_level_monitor with a schedule of every 1 MINUTES, the brute-force query as a composite aggregation as its input, and the trigger params._count >= 10.
The result, each monitor run once with dryrun so that nothing is sent: brute-force(10) 1 alert, true; spraying(8) 1, true; success-after-failures(20) 2, true; privilege-change 2, 1 true; and impossible-travel, done in code, 4, 3 true. Together 10 alerts, 8 true and 2 false; attacks A, B, C and D are all noticed. A monitor's time is the start of its bucket, so spraying shows 10:00 and brute force 12:00.
Impossible travel is code because a query cannot say "this person's previous login": the 772 events are read back in time order with search_after, and ImpossibleTravelRule decides (4 alerts). 4 monitors are saved, each on a 1-minute schedule.

### 5. The bill

The first input is 50 events in an index with no mapping, grouped by user. The result: status 400, "Text fields are not optimised for operations that require per-document field data". Grouped by user.keyword, which dynamic mapping adds next to the text, the result is status 200 and 3 groups. The mapping decides what a rule can say.
The second input is 30 failures for u077, one every 8 seconds, from 13:28:00 to 13:31:52, crossing 13:30. In fixed 5-minute buckets they are 15 in 13:25-13:30 and 15 in 13:30-13:35, each under 20, the same counts worked out by hand, so the rule gives 0 alerts: the attack falls between two buckets.
A sliding window of 5 minutes, the plain project's rule, gives 1 alert at 13:30:32, the 20th failure (13:28:00 + 19 x 8 s). And a monitor runs every minute on fixed buckets, so it notices up to a minute and a bucket late; a stopped schedule or a query edited in the console leaves no trace in the code.

## The verdict

Map the fields before loading, write each detection as a query with a threshold, tune against known attacks, run monitors on a schedule and keep their definitions in version control, do sequences in code or in an engine that has them, and test each rule with a known bad day.

## How to recognise this in code you did not write

- `date_histogram` and `bucket_selector` in a saved search.
- A monitor with a trigger in an alerting plugin.
- A threshold that someone tuned last year.
- An alert that fires every Monday at three.

## Where you have already met this

- A Kibana or OpenSearch Dashboards "Discover" screen.
- A saved search with a count and a threshold.
- A rule list in a SIEM with names like "Brute force" and "Impossible travel".
- A false alert at three in the morning.
