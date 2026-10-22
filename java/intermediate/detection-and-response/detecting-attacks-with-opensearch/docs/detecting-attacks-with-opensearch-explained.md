# Detecting Attacks with OpenSearch, Explained

## The idea in one sentence

A detection is a question asked of the log on a schedule; a query can count and group, but it cannot remember the previous event.

## The 5 acts

### 1. One Monday of logs, in OpenSearch

772 events are loaded with the bulk API: 768 logins and 4 role grants; 113 of them belong to 4 real attacks (A: spraying, B: brute force, C: impossible travel, D: privilege change). The attack label is in the index only so that this lesson can score its rules; no rule reads it. The fields the rules filter and group by are keywords and the time is a date, set by a mapping before the first event.

### 2. A first version of each rule, as a query

Brute-force(3): 3 alerts, 1 true, 2 false (the two people back from holiday). Spraying(8): 1 alert, true. Privilege-change: 2 alerts, 1 true. Success-after-failures(1): 23 alerts, 2 true. A rule is a query: a filter, a group by the subject, a bucket of time, and a threshold that keeps the buckets worth looking at.

### 3. Tuning one threshold

brute-force at 3, 5, 10, 25 and 40: 3 alerts with 1 true at 3 and 5; 1 alert, true, at 10 and 25; nothing at 40, where the attack is lost. Changing the threshold is changing one number in the query; the table says what it costs and what it misses.

### 4. The tuned set as monitors of the alerting plugin

The tuned set is saved as monitors of the alerting plugin, each with a schedule of one minute, and each is run once with dryrun, so that nothing is sent. brute-force(10): 1 alert, true; spraying(8): 1, true; success-after-failures(20): 2, true; privilege-change: 2, 1 true. Impossible travel compares each login with the same person's last one: a query cannot say the previous one, so the events are read back from OpenSearch in order and a few lines of code decide: 4 alerts, 3 true. Together: 10 alerts for the day, 8 true, 2 false; all four attacks noticed.

### 5. The bill

The same events loaded with no mapping, grouped by user: 400, text fields are not optimised for aggregations. Grouped by user.keyword, which dynamic mapping adds next to the text: 200 and 3 groups. Thirty failures in four minutes that cross a five-minute boundary at 13:30, with the rule at least 20: fixed buckets give 0 alerts, a sliding window gives 1. The monitors run on a schedule, so an attack is noticed up to a minute and a bucket later. Keep the queries as files under version control, test each with a known bad day, and treat the thresholds as tuned for this one Monday.

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
