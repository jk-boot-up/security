# Threat Hunting, Explained

## The idea in one sentence

A hunt asks the data a question that an alert never asked.

## The 5 acts

### 1. The week

40 workstations and 5 servers make lookups, start programs and log in for a week. Hidden in it are a beacon from one workstation, a document that starts a shell, a bulk upload from a server and a stolen service account. A fifth attack, a scheduled task kept on a server, is in no log.

### 2. Hunt 1: a timer in the lookups

Looking for calls at regular intervals finds every update check and time sync: 87 leads. Adding the condition that at most 3 hosts call the domain leaves 2: the beacon, calling every 300 seconds with 10% jitter, and a monitoring heartbeat. Asking for a variation under 1% keeps the perfect heartbeat and loses the beacon. After the analyst allowlists the heartbeat, one lead is left, and it is an attack.

### 3. Hunts 2 and 3: pairs and accounts

Counting parent and child program pairs and reading the rarest: a document starting a shell, the shell asking who the user is, and an administrator's ipconfig. For logins, a new address after the first four days gives 2 leads; also requiring an hour the account has never been seen gives 1: the backup account at three in the morning from an address it has never used.

### 4. Hunt 4 and the scoreboard

A host-day of more than 50 MB and far above that host's own median gives 2 leads: the upload from a server, and a patch download. In all there are 7 leads to review, which contain every attack that the logs can show. The scheduled task is not in any log, so no hunt finds it: that is a gap in the data.

### 5. From a hunt to a rule

The beacon hunt becomes a rule with the same thresholds. On a clean week it raises 1 alert, the monitoring heartbeat, and with the allowlist none. On the attack week it raises 1 alert and it is true. A hunt that worked becomes a rule with its allowlist and its tests.

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
