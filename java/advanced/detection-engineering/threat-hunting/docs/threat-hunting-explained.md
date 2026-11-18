# Threat Hunting, Explained

## The idea in one sentence

A hunt asks the data a question that an alert never asked.

## The 5 acts

### 1. The week

The input: Generator makes a week at a bank from seed 20261023: 40 workstations, 5 servers, 60 people and one service account, with 74831 name lookups, 11204 process starts and 803 logins.
The truth that every lead is judged by: a beacon from wks-014 (1152 calls, days 2 to 5), a document that starts a shell, 2400 MB sent out by srv-03 on the fifth night, and the backup account used from 203.0.113.50 at 03:07. A fifth attack, a scheduled task on a server, is in no log.
Planted on purpose, five benign look-alikes: a partner heartbeat, a nightly 600 MB transfer, a 300 MB patch download, an administrator's ipconfig, and a manager in a hotel.

### 2. Hunt 1: a timer in the lookups

The hypothesis: malware calls home on a timer, so many calls with even gaps. For each host and domain, the gaps between calls are worked out: their mean, their standard deviation (sd), and the variation, sd divided by mean. The beacon: 1152 calls, mean 300 s, sd 18 s, variation 0.06. The partner heartbeat: 0.00. An hourly update check: 0.00. A news site: 0.99, random.
Before rarity: 50 or more calls with a variation under 0.30 gives 87 leads, every time sync and update check. After rarity (at most 3 hosts call the domain): 2 leads, the beacon and the heartbeat. Too strict (under 0.01): the beacon's jitter of 0.06 slips through. After the analyst allowlists the heartbeat: 1 lead, an attack.

### 3. Hunts 2 and 3: pairs and accounts

Hunt 2. The hypothesis: an intruder runs programs that nobody else runs. The 13 parent and child pairs of the week are counted: chrome starting chrome 1160 times, and at the bottom cmd starting ipconfig twice, powershell starting whoami once and winword starting powershell once. Pairs seen at most twice give 3 leads, 2 of them attacks; ipconfig was an administrator.
Hunt 3. The hypothesis: a stolen account logs in at a new hour from a new place. Each account's baseline is learned from days 0 to 3: svc-backup from 10.2.0.9 at hours 00 and 02; user12 from 10.1.1.12 between 09 and 15. On days 4 to 6, two logins come from a new address: user12 from a hotel at hour 11 (a usual hour) and svc-backup from 203.0.113.50 at hour 03 (a new hour). Requiring both gives 1 lead, the attack.

### 4. Hunt 4 and the scoreboard

Hunt 4. The hypothesis: data leaves in bulk, far above the host's own normal. For each host, the megabytes (MB) sent out per day are compared with that host's median day plus 6 times the median absolute deviation (MAD). srv-03: median 0.78 MB, MAD 0.09 MB, limit 1.32 MB, and 2401 MB on day 5. srv-04: limit 1.46 MB and 301 MB on day 6, a patch. srv-02 sends about 601 MB every day, which is its own median, so it is no lead.
The scoreboard: 7 leads to review, 5 of them attack events, which cover 4 of the 4 attacks that the logs show. The scheduled task is in no log: a gap in the data, not in the hunt.

### 5. From a hunt to a rule

The beacon hunt becomes a rule with the same limits: 50 or more calls, a variation under 0.30, at most 3 hosts. Before the allowlist, on a clean week made from seed 20261024, it raises 1 alert, the partner heartbeat: a false alarm. After the allowlist, the clean week gives 0 alerts and the attack week 1, the beacon: a true alarm.
A hunt gives leads, not verdicts; a hunt can only look at data that was collected; and a hunt that worked becomes a rule, with an allowlist and tests.

## The verdict

Start from a hypothesis, query the data you have, judge every lead against what really happened, allowlist what is benign and write down why, name the data you are missing, and turn what works into a rule with tests.

## How to recognise this in code you did not write

- A hunt that is a search for an indicator from the news.
- A beacon rule with no allowlist.
- A threshold nobody has tuned.
- A hunt in one person's notebook.

## Where you have already met this

- A search for a bad IP address from a news article.
- A dashboard of top talkers that nobody reads.
- An alert threshold set once and never tuned.
- A hunt done by one person and never written down.
