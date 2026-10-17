# The CIA Triad, Explained

## The idea in one sentence

The CIA triad says that information must stay private (confidentiality), stay correct (integrity), and stay reachable (availability), and that every security control protects at least one of the three.

## The 5 acts

### 1. The first version, with no controls

The first version of the order system trusts everyone. Raj, from support, reads
order O-101 and gets the whole record, including the shipping address, 12 Palm
Street, which he never needed: a leak. Mallory changes the amount from $1200 to
$12 and nothing records it, so when Ana reads the order she sees $12 and nobody
knows it was changed: a silent change. Then 101 checkout requests arrive in one
tick, 100 from a bot and one from a customer, Maya. The server can do 10 per
tick and serves them in order, so it serves 10 of the bot's and refuses Maya's:
an outage for the person who wanted to buy.

### 2. Confidentiality: each role sees what it needs

The fix for the leak is to decide what each role may see. The order system now
looks at the asker's role before it answers. Raj, from support, gets the
customer and the item, and the address and the amount come back hidden. Ana,
from finance, gets everything. Mallory tries to change an amount and is denied,
because only finance may. The principle is called least privilege: give each
person only what the job needs, and nothing more.

### 3. Integrity: a change behind the application is caught

Role checks protect the front door, but someone may reach the stored data some
other way. So every order is saved together with a seal: a short code worked
out from the order and a secret key. On every read, the system works the code
out again and compares. Mallory edits the amount directly in storage, from
$1200 to $12, and leaves the seal as it was. When Ana reads the order, the
codes do not match, and the system refuses to show it instead of showing a
wrong amount. Other orders, such as O-102, are unaffected. The seal does not
stop the edit; it makes sure the edit cannot go unnoticed.

### 4. Availability: one client cannot use up the server

The same flash-sale flood arrives again: 100 requests from the bot and one from
Maya. This time the server first asks a rate limiter, which allows each client 3
requests per tick. The bot is served 3 times and refused 97. Maya's single
request is served. Availability is protected by sharing the capacity fairly, so
one client, careless or hostile, cannot use all of it.

### 5. The bill: strict controls get in the way of emergencies

Strict confidentiality has a cost. A courier is standing at the wrong door, the
customer is on the phone, finance is not available, and Raj cannot see the
shipping address that would let him fix it. The system offers an emergency
exception, usually called break-glass access: any employee may read the
address, but must give a reason, and the use is written to an audit log. Raj
gets the address, 12 Palm Street, and the log now has one entry saying who used
the exception, on which order, and why. The three promises pull against each
other, so the exception must exist, and the exception must be watched.

## The verdict

The triad is a checklist for thinking, not a product. Whenever a system holds
something that matters, ask three questions: who must not see it, who must not
change it, and who must always be able to reach it. Then ask which control
answers each question, and what that control costs the other two promises.

## How to recognise this in code you did not write

- A permission check before returning data (confidentiality).
- A checksum, signature or seal stored beside data (integrity).
- A rate limit, a backup or a second server (availability).
- An emergency exception with a logged reason: the triad's promises being traded off on purpose.

## Where you have already met this

- A support screen that shows an order's item but not the card or the address (confidentiality).
- A download page that shows a checksum beside the file, so you can tell if it was altered (integrity).
- A shop that goes down on a sale day, and the protection services that absorb the flood of traffic (availability).
- The phrase "break glass in case of emergency" on a fire alarm, and its twin in business software.
